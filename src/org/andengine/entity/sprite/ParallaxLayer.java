package org.andengine.entity.sprite;

/**
 * Original version of this file is from:
 * http://www.andengine.org/forums/features/scrollable-parallax-background-t5390.html
 */

import java.util.ArrayList;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.Entity;
import org.andengine.entity.shape.IAreaShape;
import org.andengine.opengl.util.GLState;

public class ParallaxLayer extends Entity {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private final ArrayList<ParallaxEntity> mParallaxEntities = new ArrayList<ParallaxEntity>();
	private int mParallaxEntityCount;

	protected float mParallaxValue;
	protected float mParallaxScrollValue;
	
	protected float mParallaxChangePerSecond;
	
	protected float mParallaxScrollFactor = 0.2f;
	
	private Camera mCamera;
	
	private float mCameraPreviousX;
	private float mCameraOffsetX;
	
	private float	mLevelWidth = 0;
	
	private boolean mIsScrollable = false;

	
	// ===========================================================
	// Constructors
	// ===========================================================
	public ParallaxLayer() {
	}

	public ParallaxLayer(final Camera pCamera, final boolean pIsScrollable){
		this.mCamera = pCamera;
		this.mIsScrollable = pIsScrollable;
		
		mCameraPreviousX = pCamera.getCenterX();
	}
	
	public ParallaxLayer(final Camera pCamera, final boolean pIsScrollable, final int pLevelWidth){
		this.mCamera = pCamera;
		this.mIsScrollable = pIsScrollable;
		this.mLevelWidth = pLevelWidth;
		
		mCameraPreviousX = pCamera.getCenterX();
	}
	
	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setParallaxValue(final float pParallaxValue) {
		this.mParallaxValue = pParallaxValue;
	}
	
	public void setParallaxChangePerSecond(final float pParallaxChangePerSecond) {
		this.mParallaxChangePerSecond = pParallaxChangePerSecond;
	}

	public void setParallaxScrollFactor(final float pParallaxScrollFactor){
		this.mParallaxScrollFactor = pParallaxScrollFactor;
	}
	
	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	public void onManagedDraw(GLState pGLState, Camera pCamera) {
		super.preDraw(pGLState, pCamera);

		
		final float parallaxValue = this.mParallaxValue;
		final float parallaxScrollValue = this.mParallaxScrollValue;
		final ArrayList<ParallaxEntity> parallaxEntities = this.mParallaxEntities;

		for(int i = 0; i < this.mParallaxEntityCount; i++) {
			if(parallaxEntities.get(i).mIsScrollable){
				parallaxEntities.get(i).onDraw(pGLState, pCamera, parallaxScrollValue, mLevelWidth);
			} else {
				parallaxEntities.get(i).onDraw(pGLState, pCamera, parallaxValue, mLevelWidth);
			}

		}
	}
	
	@Override
	protected void onManagedUpdate(float pSecondsElapsed) {
		
		if(mIsScrollable && mCameraPreviousX != this.mCamera.getCenterX()){
				mCameraOffsetX = mCameraPreviousX - this.mCamera.getCenterX();
				mCameraPreviousX = this.mCamera.getCenterX();
				
				this.mParallaxScrollValue += (mCameraOffsetX * this.mParallaxScrollFactor);
				mCameraOffsetX = 0;
		}
		
		this.mParallaxValue += this.mParallaxChangePerSecond * pSecondsElapsed;
		super.onManagedUpdate(pSecondsElapsed);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void attachParallaxEntity(final ParallaxEntity parallaxEntity) {
		this.mParallaxEntities.add(parallaxEntity);
		this.mParallaxEntityCount++;
	}

	public boolean detachParallaxEntity(final ParallaxEntity pParallaxEntity) {
		this.mParallaxEntityCount--;
		final boolean success = this.mParallaxEntities.remove(pParallaxEntity);
		if(!success) {
			this.mParallaxEntityCount++;
		}
		return success;
	}
	
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	public static class ParallaxEntity {
		// ===========================================================
		// Constants
		// ===========================================================

		// ===========================================================
		// Fields
		// ===========================================================

		final float mParallaxFactor;
		final IAreaShape mAreaShape;
		final boolean mIsScrollable;

		final float mShapeWidthScaled;

		// ===========================================================
		// Constructors
		// ===========================================================

		public ParallaxEntity(final float pParallaxFactor, final IAreaShape pAreaShape) {
			this.mParallaxFactor = pParallaxFactor;
			this.mAreaShape = pAreaShape;
			this.mIsScrollable = false;
			mShapeWidthScaled = this.mAreaShape.getWidthScaled();
		}
		
		public ParallaxEntity(final float pParallaxFactor, final IAreaShape pAreaShape, final boolean pIsScrollable) {
			this.mParallaxFactor = pParallaxFactor;
			this.mAreaShape = pAreaShape;
			this.mIsScrollable = pIsScrollable;
			mShapeWidthScaled = this.mAreaShape.getWidthScaled();
		}
		
		public ParallaxEntity(final float pParallaxFactor, final IAreaShape pAreaShape, final boolean pIsScrollable, final int pReduceFrequency) {
			this.mParallaxFactor = pParallaxFactor;
			this.mAreaShape = pAreaShape;
			this.mIsScrollable = pIsScrollable;
			mShapeWidthScaled = this.mAreaShape.getWidthScaled() * pReduceFrequency;
		}

		// ===========================================================
		// Getter & Setter
		// ===========================================================

		// ===========================================================
		// Methods for/from SuperClass/Interfaces
		// ===========================================================

		// ===========================================================
		// Methods
		// ===========================================================

		public void onDraw(final GLState pGLState, final Camera pCamera, final float pParallaxValue, final float pLevelWidth) {
			pGLState.pushModelViewGLMatrix();
			{
				float widthRange;
				
				if(pLevelWidth != 0){
					widthRange = pLevelWidth;
				} else {
					widthRange = pCamera.getWidth();
				}

				float baseOffset = (pParallaxValue * this.mParallaxFactor) % mShapeWidthScaled;

				while(baseOffset > 0) {
					baseOffset -= mShapeWidthScaled;
				}
				pGLState.translateModelViewGLMatrixf(baseOffset, 0, 0);

				float currentMaxX = baseOffset;
				
				do {
					this.mAreaShape.onDraw(pGLState, pCamera);
					pGLState.translateModelViewGLMatrixf(mShapeWidthScaled - 1, 0, 0);
					currentMaxX += mShapeWidthScaled;
				} while(currentMaxX < widthRange);
			}
			pGLState.popModelViewGLMatrix();
		}

		// ===========================================================
		// Inner and Anonymous Classes
		// ===========================================================
	}


}