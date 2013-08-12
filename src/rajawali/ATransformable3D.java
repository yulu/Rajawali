/**
 * Copyright 2013 Dennis Ippel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package rajawali;

import rajawali.bounds.IBoundingVolume;
import rajawali.math.Matrix4;
import rajawali.math.Quaternion;
import rajawali.math.vector.Vector3;
import rajawali.math.vector.Vector3.Axis;
import rajawali.renderer.AFrameTask;
import rajawali.scenegraph.IGraphNode;
import rajawali.scenegraph.IGraphNodeMember;

public abstract class ATransformable3D extends AFrameTask implements IGraphNodeMember {
	protected final Vector3 mPosition;
	protected final Vector3 mScale;
	protected final Quaternion mOrientation;
	protected final Quaternion mTmpOrientation;
	protected Vector3 mLookAt;
	protected boolean mIsCamera;
	
	protected IGraphNode mGraphNode;
	protected boolean mInsideGraph = false; //Default to being outside the graph
	
	/**
	 * Default constructor for {@link ATransformable3D}.
	 */
	public ATransformable3D() {
		mPosition = new Vector3();
		mScale = new Vector3(1, 1, 1);
		mOrientation = new Quaternion();
		mTmpOrientation = new Quaternion();
	}
	
	
	
	//--------------------------------------------------
	// Translation Methods
	//--------------------------------------------------
	
	/**
	 * Sets the position of this {@link ATransformable3D}. If this is 
	 * part of a scene graph, the graph will be notified of the change.
	 * 
	 * @param position {@link Vector3} The new position. This is copied
	 * into an internal store and can be used after this method returns.
	 */
	public void setPosition(Vector3 position) {
		mPosition.setAll(position);
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	/**
	 * Sets the position of this {@link ATransformable3D}. If this is 
	 * part of a scene graph, the graph will be notified of the change.
	 * 
	 * @param x double The x coordinate new position.
	 * @param y double The y coordinate new position.
	 * @param z double The z coordinate new position.
	 */
	public void setPosition(double x, double y, double z) {
		mPosition.setAll(x, y, z);
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	/**
	 * Sets the x component of the position for this {@link ATransformable3D}.
	 * 
	 * @param x double The new x component for the position.
	 */
	public void setX(double x) {
		mPosition.x = x;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}
	
	/**
	 * Sets the y component of the position for this {@link ATransformable3D}.
	 * 
	 * @param y double The new y component for the position.
	 */
	public void setY(double y) {
		mPosition.y = y;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}
	
	/**
	 * Sets the z component of the position for this {@link ATransformable3D}.
	 * 
	 * @param z double The new z component for the position.
	 */
	public void setZ(double z) {
		mPosition.z = z;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}
	
	public Vector3 getPosition() {
		return mPosition;
	}
	
	public double getX() {
		return mPosition.x;
	}

	public double getY() {
		return mPosition.y;
	}

	public double getZ() {
		return mPosition.z;
	}
	
	/**
	 * Rotates this {@link ATransformable3D} by the rotation described by the provided
	 * {@link Quaternion}.
	 * 
	 * @param quat {@link Quaternion} describing the additional rotation.
	 * @return A reference to this {@link ATransformable3D} to facilitate chaining.
	 */
	public ATransformable3D rotate(final Quaternion quat) {
		mOrientation.multiply(quat);
		return this;
	}
	
	/**
	 * Rotates this {@link ATransformable3D} by the rotation described by the provided
	 * {@link Vector3} axis and angle of rotation.
	 * 
	 * @param axis {@link Vector3} The axis of rotation.
	 * @param angle double The angle of rotation in degrees.
	 * @return A reference to this {@link ATransformable3D} to facilitate chaining.
	 */
	public ATransformable3D rotate(final Vector3 axis, double angle) {
		mOrientation.multiply(mTmpOrientation.fromAngleAxis(axis, angle));
		return this;
	}
	
	/**
	 * Rotates this {@link ATransformable3D} by the rotation described by the provided
	 * {@link Axis} cardinal axis and angle of rotation.
	 * 
	 * @param axis {@link Axis} The axis of rotation.
	 * @param angle double The angle of rotation in degrees.
	 * @return A reference to this {@link ATransformable3D} to facilitate chaining.
	 */
	public ATransformable3D rotate(final Axis axis, double angle) {
		mOrientation.multiply(mTmpOrientation.fromAngleAxis(axis, angle));
		return this;
	}
	
	/**
	 * Rotates this {@link ATransformable3D} by the rotation described by the provided
	 * axis and angle of rotation.
	 * 
	 * @param x double The x component of the axis of rotation.
	 * @param y double The y component of the axis of rotation.
	 * @param z double The z component of the axis of rotation.
	 * @param angle double The angle of rotation in degrees.
	 * @return A reference to this {@link ATransformable3D} to facilitate chaining.
	 */
	public ATransformable3D rotate(double x, double y, double z, double angle) {
		mOrientation.multiply(mTmpOrientation.fromAngleAxis(x, y, z, angle));
		return this;
	}
	
	/**
	 * Rotates this {@link ATransformable3D} by the rotation described by the provided
	 * {@link Matrix4}.
	 * 
	 * @param matrix {@link Matrix4} describing the rotation to apply.
	 * @return A reference to this {@link ATransformable3D} to facilitate chaining.
	 */
	public ATransformable3D rotate(final Matrix4 matrix) {
		mOrientation.multiply(mTmpOrientation.fromMatrix(matrix));
		return this;
	}
	
	public void rotateAround(Vector3 axis, double angle) {
		rotateAround(axis, angle, true);
	}
	
 	public void rotateAround(Vector3 axis, double angle, boolean append) {
 		if(append) {
 			mTmpOrientation.fromAngleAxis(axis, angle);
 			mOrientation.multiply(mTmpOrientation);
 		} else {
 			mOrientation.fromAngleAxis(axis, angle);
 		}
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}
	
	public Quaternion getOrientation(Quaternion qt) {
		qt.setAll(mOrientation); 
		return  qt;
	}
	
	public void setOrientation(Quaternion quat) {
		mOrientation.setAll(quat);
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}
	
	public void setRotation(double rotX, double rotY, double rotZ) {
		mOrientation.fromEuler(rotY, rotZ, rotX);
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}
	
	public void setRotX(double rotX) {
		mTmpOrientation.setAll(mOrientation);
		mOrientation.fromEuler(mTmpOrientation.getYaw(false), mTmpOrientation.getPitch(false), rotX);
	}

	public double getRotX() {
		return mOrientation.getRoll(false);
	}

	public void setRotY(double rotY) {
		mTmpOrientation.setAll(mOrientation);
		mOrientation.fromEuler(rotY, mTmpOrientation.getPitch(false), mTmpOrientation.getRoll(false));
	}

	public double getRotY() {
		return mOrientation.getYaw(false);
	}

	public void setRotZ(double rotZ) {
		mTmpOrientation.setAll(mOrientation);
		mOrientation.fromEuler(mTmpOrientation.getYaw(false), rotZ, mTmpOrientation.getRoll(false));
		mOrientation.normalize();
	}

	public double getRotZ() {
		return mOrientation.getPitch(false);
	}
	
	public void setRotation(Vector3 rotation) {
		mOrientation.fromEuler(rotation.y, rotation.z, rotation.x);
	}

	public void setScale(double scale) {
		mScale.x = scale;
		mScale.y = scale;
		mScale.z = scale;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	public void setScale(double scaleX, double scaleY, double scaleZ) {
		mScale.x = scaleX;
		mScale.y = scaleY;
		mScale.z = scaleZ;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	public void setScaleX(double scaleX) {
		mScale.x = scaleX;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	public double getScaleX() {
		return mScale.x;
	}

	public void setScaleY(double scaleY) {
		mScale.y = scaleY;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	public double getScaleY() {
		return mScale.y;
	}

	public void setScaleZ(double scaleZ) {
		mScale.z = scaleZ;
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	public double getScaleZ() {
		return mScale.z;
	}
	
	public Vector3 getScale() {
		return mScale;
	}

	public void setScale(Vector3 scale) {
		mScale.setAll(scale);
		if (mGraphNode != null) mGraphNode.updateObject(this);
	}

	public Vector3 getLookAt() {
		return mLookAt;
	}
	
	public void setLookAt(double x, double y, double z) {
		if (mLookAt == null) mLookAt = new Vector3();
		mLookAt.x = x;
		mLookAt.y = y;
		mLookAt.z = z;
		//mRotationDirty = true;
	}
	
	public void setLookAt(Vector3 lookAt) {
		if (lookAt == null) {
			mLookAt = null;
			return;
		}
		setLookAt(lookAt.x,  lookAt.y, lookAt.z);
	}

	/*
	 * (non-Javadoc)
	 * @see rajawali.scenegraph.IGraphNodeMember#setGraphNode(rajawali.scenegraph.IGraphNode)
	 */
	public void setGraphNode(IGraphNode node, boolean inside) {
		mGraphNode = node;
		mInsideGraph = inside;
	}
	
	/*
	 * (non-Javadoc)
	 * @see rajawali.scenegraph.IGraphNodeMember#getGraphNode()
	 */
	public IGraphNode getGraphNode() {
		return mGraphNode;
	}
	
	/*
	 * (non-Javadoc)
	 * @see rajawali.scenegraph.IGraphNodeMember#isInGraph()
	 */
	public boolean isInGraph() {
		return mInsideGraph;
	}
	
	/*
	 * (non-Javadoc)
	 * @see rajawali.scenegraph.IGraphNodeMember#getTransformedBoundingVolume()
	 */
	public IBoundingVolume getTransformedBoundingVolume() {
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see rajawali.scenegraph.IGraphNodeMember#getScenePosition()
	 */
	public Vector3 getScenePosition() {
		return mPosition;
	}
}
