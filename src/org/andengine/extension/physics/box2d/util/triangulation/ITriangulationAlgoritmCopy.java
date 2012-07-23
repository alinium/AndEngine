package org.andengine.extension.physics.box2d.util.triangulation;

import java.util.List;

import com.badlogic.gdx.math.Vector2Copy;

/**
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich
 * @since 20:16:04 - 14.09.2010
 */
public interface ITriangulationAlgoritmCopy {
	// ===========================================================
	// Final Fields
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * @return a {@link List} of {@link Vector2Copy} objects where every three {@link Vector2Copy} objects form a triangle.
	 */
	public List<Vector2Copy> computeTriangles(final List<Vector2Copy> pVertices);
}
