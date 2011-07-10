#include <string.h>
#include <jni.h>
#include <android/log.h>

#include <btBulletDynamicsCommon.h>
#include <btBulletCollisionCommon.h>
#include "BulletMultiThreaded/SpuNarrowPhaseCollisionTask/SpuGatheringCollisionTask.h"
#include "BulletMultiThreaded/btParallelConstraintSolver.h"
#include "BulletMultiThreaded/SequentialThreadSupport.h"
#include "BulletMultiThreaded/SpuGatheringCollisionDispatcher.h"
#include "BulletCollision/CollisionDispatch/btSimulationIslandManager.h"


//#define USE_PARALLEL_COLLISION
//#define USE_PARALLEL_CONSTRAINT
//#define USZE_AS3_BROADPHASE


btDiscreteDynamicsWorld *g_DynamicsWorld;

btRigidBody *g_rb[4096];
btDefaultMotionState *g_ms[4096];
btTransform *g_pos[4096];
int g_ptr;

btGeneric6DofSpringConstraint *g_cst[4096];
int g_cptr;

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_initFaceNative(JNIEnv* env, jobject thiz, jobject vertex, jint count, jobject index, jobject offset)
{
	float* vert = (float*)env->GetDirectBufferAddress(vertex);
	int*   idx  = (int*)  env->GetDirectBufferAddress(index);
	float* ofs  = (float*)env->GetDirectBufferAddress(offset);
	
	for(int i = 0; i < count; i++) {
		for(int j = 0; j < 3; j++) {
			vert[idx[i] * 8 + j] = ofs[i * 3 + j];
		}
	}
	
	return ;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_setFaceNative(JNIEnv* env, jobject thiz, jobject vertex, jobject pointer, jint count, jobject index, jobject offset, jfloat weight)
{
	float* vert = (float*)env->GetDirectBufferAddress(vertex);
	int*   ptr  = (int*)  env->GetDirectBufferAddress(pointer);
	int*   idx  = (int*)  env->GetDirectBufferAddress(index);
	float* ofs  = (float*)env->GetDirectBufferAddress(offset);
	
	for(int i = 0; i < count; i++) {
		for(int j = 0; j < 3; j++) {
			vert[ptr[idx[i]] * 8 + j] += ofs[i * 3 + j] * weight;
		}
	}
	
	return ;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_CoreLogic_btMakeWorld(JNIEnv* env, jobject thiz)
{
	// make world
	////// Collision Configuration
	btDefaultCollisionConstructionInfo cci;
	cci.m_defaultMaxPersistentManifoldPoolSize = 32768;
	btCollisionConfiguration *collisionConfiguration = new btDefaultCollisionConfiguration(cci);

	////// Collision Dispatcher
#ifdef USE_PARALLEL_COLLISION
	SequentialThreadSupport::SequentialThreadConstructionInfo colCI("collision", processCollisionTask, createCollisionLocalStoreMemory);
	SequentialThreadSupport* threadSupportCollision = new SequentialThreadSupport(colCI);
	SpuGatheringCollisionDispatcher *dispatcher = new SpuGatheringCollisionDispatcher(threadSupportCollision, 4, collisionConfiguration);
#else
	btCollisionDispatcher *dispatcher = new btCollisionDispatcher(collisionConfiguration);
#endif

	////// Broadphase
#ifdef USZE_AS3_BROADPHASE
	btVector3 worldAabbMin(-1000,-1000,-1000);
	btVector3 worldAabbMax(1000,1000,1000);
	btAxisSweep3 *broadphase = new btAxisSweep3(worldAabbMin, worldAabbMax);
#else
	btDbvtBroadphase *broadphase = new btDbvtBroadphase();
#endif

	////// Constraint Solver
#ifdef USE_PARALLEL_CONSTRAINT
	SequentialThreadSupport :: SequentialThreadConstructionInfo tci("solverThreads", SolverThreadFunc, SolverlsMemoryFunc);
	SequentialThreadSupport* threadSupport = new SequentialThreadSupport(tci);
	threadSupport->startSPU();
	btParallelConstraintSolver *solver = new btParallelConstraintSolver(threadSupport);
	dispatcher->setDispatcherFlags(btCollisionDispatcher::CD_DISABLE_CONTACTPOOL_DYNAMIC_ALLOCATION);
#else
	btSequentialImpulseConstraintSolver *solver = new btSequentialImpulseConstraintSolver();
#endif
	
	
	////// Create Dynamics World
	g_DynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
	
	g_DynamicsWorld->getSimulationIslandManager()->setSplitIslands(false);
	g_DynamicsWorld->getSolverInfo().m_numIterations = 4;
	g_DynamicsWorld->getSolverInfo().m_solverMode = SOLVER_SIMD + SOLVER_USE_WARMSTARTING;
	g_DynamicsWorld->getDispatchInfo().m_enableSPU = true;

	btVector3 *g = new btVector3(0, -9.8 * 5, 0);
	g_DynamicsWorld->setGravity(*g);
	
	g_ptr = 0;
	g_cptr = 0;
}

btVector3 createBtVector3(JNIEnv* env, jfloatArray vec)
{
	float* vec_n = env->GetFloatArrayElements(vec, 0);
	float x = vec_n[0];
	float y = vec_n[1];
	float z = vec_n[2];
	env->ReleaseFloatArrayElements(vec, vec_n, 0);
	
	return btVector3(x, y, z);
}

btMatrix3x3 createBtMatrix3x3(JNIEnv* env, jfloatArray rot)
{
	float* rot_n = env->GetFloatArrayElements(rot, 0);
	float rx = rot_n[0];
	float ry = rot_n[1];
	float rz = rot_n[2];
	
	btMatrix3x3 mat;
	mat.setIdentity();
	mat.setEulerZYX(rx, ry, rz);
	
	env->ReleaseFloatArrayElements(rot, rot_n, 0);	
	
	return mat;
}

btTransform createBtTransform(JNIEnv* env, jfloatArray pos, jfloatArray rot)
{
	return btTransform(createBtMatrix3x3(env, rot), createBtVector3(env, pos));
}


extern "C" jint Java_jp_gauzau_MikuMikuDroid_Miku_btAddRigidBody(JNIEnv* env, jobject thiz,
		jint type, jint shape,
		jfloat w, jfloat h, jfloat d,
		jfloatArray pos, jfloatArray rot, jfloatArray head_pos, jfloatArray bone,
		jfloat mass, jfloat v_dim, jfloat r_dim, jfloat recoil, jfloat friction,
		jbyte group_index, jshort group_target)
{
	// create CollisionShape
	btCollisionShape* cs;
	switch(shape) {
	case 0: //sphere
		cs = new btSphereShape(w);
		break;
	case 1: // box
		cs = new btBoxShape(btVector3(w, h, d));
		break;
	case 2: // capsule
		cs = new btCapsuleShape(w, h);
		break;
	default:
		cs = 0;	// NullPointerException
		break;
	}

	// position and rotation
	btTransform transf = createBtTransform(env, pos, rot);
	btMatrix3x3 norot;
	norot.setIdentity();
	g_pos[g_ptr] = new btTransform(btTransform(norot, createBtVector3(env, head_pos)) * transf);
	
	float* bone_native = env->GetFloatArrayElements(bone, 0);
	btTransform tr;
	tr.setFromOpenGLMatrix(bone_native);
	env->ReleaseFloatArrayElements(bone, bone_native, 0);
	transf = tr * transf;

	// inertia
	btVector3 inertia(0, 0, 0);
	if(type != 0) {
		cs->calculateLocalInertia(mass, inertia);
	}
	
	// create rigid body with default motion state
	g_ms[g_ptr] = new btDefaultMotionState(transf);
	
	btRigidBody :: btRigidBodyConstructionInfo* rbi = new btRigidBody :: btRigidBodyConstructionInfo(type == 0 ? 0 : mass, g_ms[g_ptr], cs, inertia);
	rbi->m_linearDamping = v_dim;
	rbi->m_angularDamping = r_dim;
	rbi->m_restitution = recoil;
	rbi->m_friction = friction;
	rbi->m_linearSleepingThreshold = 0;
	rbi->m_angularSleepingThreshold = 0;
	
	g_rb[g_ptr] = new btRigidBody(*rbi);
	
	if(type == 0) {
		g_rb[g_ptr]->setActivationState(DISABLE_DEACTIVATION);
		g_rb[g_ptr]->setCollisionFlags(g_rb[g_ptr]->getCollisionFlags() | btCollisionObject :: CF_KINEMATIC_OBJECT);
	}

	g_DynamicsWorld->addRigidBody(g_rb[g_ptr], (1 << group_index), group_target);
	
	return g_ptr++;
}

extern "C" jint Java_jp_gauzau_MikuMikuDroid_Miku_btAddJoint(JNIEnv* env, jobject thiz,
			jint rb1, jint rb2, jfloatArray pos, jfloatArray rot, jfloatArray p1, jfloatArray p2, jfloatArray r1, jfloatArray r2, jfloatArray sp, jfloatArray sr)
{
	btTransform jt = createBtTransform(env, pos, rot);
	
	btTransform tr1 = g_pos[rb1]->inverse() * jt;
	btTransform tr2 = g_pos[rb2]->inverse() * jt;
	btGeneric6DofSpringConstraint* dof = new btGeneric6DofSpringConstraint(*g_rb[rb1], *g_rb[rb2], tr1, tr2, true);
	
	dof->setLinearLowerLimit(createBtVector3(env, p1));
	dof->setLinearUpperLimit(createBtVector3(env, p2));
	dof->setAngularLowerLimit(createBtVector3(env, r1));
	dof->setAngularUpperLimit(createBtVector3(env, r2));
	
	float *sp_n = env->GetFloatArrayElements(sp, 0);
	float *sr_n = env->GetFloatArrayElements(sr, 0);	
	for(int i = 0; i < 3; i++) {
		if(sp_n[i] > 0) {
			dof->enableSpring(i, true);
			dof->setStiffness(i, sp_n[i]);
		} else {
			dof->enableSpring(i, false);
		}

		if(sr_n[i] > 0) {
			dof->enableSpring(i + 3, true);
			dof->setStiffness(i + 3, sr_n[i]);
		} else {
			dof->enableSpring(i + 3, false);
		}

	}
	env->ReleaseFloatArrayElements(sp, sp_n, 0);
	env->ReleaseFloatArrayElements(sr, sr_n, 0);
	
	g_DynamicsWorld->addConstraint(dof, true);	// disableCollisionsBetweenLinkedBodies
	g_cst[g_cptr] = dof;
	
	return g_cptr++;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_CoreLogic_btClearAllData(JNIEnv* env, jobject thiz)
{
	for(int i = 0; i < g_cptr; i++) {
		g_DynamicsWorld->removeConstraint(g_cst[i]);
		delete g_cst[i];
		g_cst[i] = 0;
	}
	
	for(int i = 0; i < g_ptr; i++) {
		g_DynamicsWorld->removeRigidBody(g_rb[i]);
		delete g_rb[i];
		delete g_ms[i];
		delete g_pos[i];
		g_rb[i] = 0;
		g_ms[i] = 0;
		g_pos[i] = 0;
	}
	
	g_ptr = 0;
	g_cptr = 0;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_CoreLogic_btStepSimulation(JNIEnv* env, jobject thiz, jfloat step, jint max)
{
	g_DynamicsWorld->stepSimulation(step, max);
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_btGetOpenGLMatrix(JNIEnv* env, jobject thiz, jint rb,	jfloatArray matrix, jfloatArray pos, jfloatArray rot)
{
	// rigid body initial position & rotation
	btTransform rbt = createBtTransform(env, pos, rot);

	// rigid body in dynamics world
	btTransform tr;
	g_ms[rb]->getWorldTransform(tr);

	tr = tr * rbt.inverse();

	float* matrix_native = env->GetFloatArrayElements(matrix, 0);
	tr.getOpenGLMatrix(matrix_native);
	env->ReleaseFloatArrayElements(matrix, matrix_native, 0);
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_btSetOpenGLMatrix(JNIEnv* env, jobject thiz, jint rb, jfloatArray matrix, jfloatArray pos, jfloatArray rot)
{
	if(rb < g_ptr) {
		// rigid body initial position & rotation
		btTransform rbt = createBtTransform(env, pos, rot);
	
		// rigid body in VMD world
		float* matrix_native = env->GetFloatArrayElements(matrix, 0);
		btTransform tr;
		tr.setFromOpenGLMatrix(matrix_native);
		env->ReleaseFloatArrayElements(matrix, matrix_native, 0);

		tr = tr * rbt;	
	
		g_ms[rb]->setWorldTransform(tr);
	}
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_CoreLogic_btDumpAll(JNIEnv* env, jobject thiz)
{
#ifndef BT_NO_PROFILE
	CProfileManager::dumpAll();
#endif
}
