# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#LOCAL_ARM_MODE := arm
#LOCAL_ARM_NEON := true

LOCAL_CPPFLAGS	:= -O3 -DNDEBUG -I./bullet -DUSE_MINICL

LOCAL_MODULE    := bullet-jni

LOCAL_SRC_FILES := bullet-jni.cpp \
./bullet/BulletCollision/BroadphaseCollision/btCollisionAlgorithm.cpp \
./bullet/BulletCollision/BroadphaseCollision/btDbvt.cpp \
./bullet/BulletCollision/BroadphaseCollision/btDbvtBroadphase.cpp \
./bullet/BulletCollision/BroadphaseCollision/btDispatcher.cpp \
./bullet/BulletCollision/BroadphaseCollision/btOverlappingPairCache.cpp \
./bullet/BulletCollision/BroadphaseCollision/btQuantizedBvh.cpp \
./bullet/BulletCollision/CollisionDispatch/btActivatingCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btBoxBoxCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btBoxBoxDetector.cpp \
./bullet/BulletCollision/CollisionDispatch/btCollisionDispatcher.cpp \
./bullet/BulletCollision/CollisionDispatch/btCollisionObject.cpp \
./bullet/BulletCollision/CollisionDispatch/btCollisionWorld.cpp \
./bullet/BulletCollision/CollisionDispatch/btCompoundCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btConvexConcaveCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btConvexConvexAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btConvexPlaneCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btDefaultCollisionConfiguration.cpp \
./bullet/BulletCollision/CollisionDispatch/btEmptyCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btManifoldResult.cpp \
./bullet/BulletCollision/CollisionDispatch/btSimulationIslandManager.cpp \
./bullet/BulletCollision/CollisionDispatch/btSphereSphereCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btSphereTriangleCollisionAlgorithm.cpp \
./bullet/BulletCollision/CollisionDispatch/btUnionFind.cpp \
./bullet/BulletCollision/CollisionDispatch/SphereTriangleDetector.cpp \
./bullet/BulletCollision/CollisionShapes/btBoxShape.cpp \
./bullet/BulletCollision/CollisionShapes/btBvhTriangleMeshShape.cpp \
./bullet/BulletCollision/CollisionShapes/btCapsuleShape.cpp \
./bullet/BulletCollision/CollisionShapes/btCollisionShape.cpp \
./bullet/BulletCollision/CollisionShapes/btConcaveShape.cpp \
./bullet/BulletCollision/CollisionShapes/btConvexInternalShape.cpp \
./bullet/BulletCollision/CollisionShapes/btConvexPolyhedron.cpp \
./bullet/BulletCollision/CollisionShapes/btConvexShape.cpp \
./bullet/BulletCollision/CollisionShapes/btOptimizedBvh.cpp \
./bullet/BulletCollision/CollisionShapes/btPolyhedralConvexShape.cpp \
./bullet/BulletCollision/CollisionShapes/btSphereShape.cpp \
./bullet/BulletCollision/CollisionShapes/btTriangleCallback.cpp \
./bullet/BulletCollision/CollisionShapes/btTriangleMeshShape.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btContinuousConvexCollision.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btConvexCast.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btGjkConvexCast.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btGjkEpa2.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btGjkEpaPenetrationDepthSolver.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btGjkPairDetector.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btMinkowskiPenetrationDepthSolver.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btSubSimplexConvexCast.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btPersistentManifold.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btPolyhedralContactClipping.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btRaycastCallback.cpp \
./bullet/BulletCollision/NarrowPhaseCollision/btVoronoiSimplexSolver.cpp \
./bullet/BulletDynamics/ConstraintSolver/btConeTwistConstraint.cpp \
./bullet/BulletDynamics/ConstraintSolver/btContactConstraint.cpp \
./bullet/BulletDynamics/ConstraintSolver/btGeneric6DofConstraint.cpp \
./bullet/BulletDynamics/ConstraintSolver/btGeneric6DofSpringConstraint.cpp \
./bullet/BulletDynamics/ConstraintSolver/btSequentialImpulseConstraintSolver.cpp \
./bullet/BulletDynamics/ConstraintSolver/btTypedConstraint.cpp \
./bullet/BulletDynamics/Dynamics/btDiscreteDynamicsWorld.cpp \
./bullet/BulletDynamics/Dynamics/btRigidBody.cpp \
./bullet/LinearMath/btConvexHull.cpp \
./bullet/LinearMath/btConvexHullComputer.cpp \
./bullet/LinearMath/btGeometryUtil.cpp \
./bullet/LinearMath/btAlignedAllocator.cpp \
./bullet/LinearMath/btQuickprof.cpp \
./bullet/LinearMath/btSerializer.cpp

include $(BUILD_SHARED_LIBRARY)

$(call import-module,cpufeatures)
