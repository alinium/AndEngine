/*******************************************************************************
 * Copyright 2010 Mario Zechner (contact@badlogicgames.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.andengine.extension.physics.box2d.util.triangulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.math.Vector2Copy;

/**
 * A simple implementation of the ear cutting algorithm to triangulate simple
 * polygons without holes. For more information:
 * @see http://cgm.cs.mcgill.ca/~godfried/teaching/cg-projects/97/Ian/algorithm2.html
 * @see http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf
 * 
 * @author badlogicgames@gmail.com
 * (c) 2010 Nicolas Gramlich 
 * (c) 2011 Zynga Inc.
 * 
 * @author Nicolas Gramlich (Improved performance. Collinear edges are now supported.)
 */
public final class EarClippingTriangulatorCopy implements ITriangulationAlgoritmCopy {
	// ===========================================================
	// Constants
	// ===========================================================

	private static final int CONCAVE = 1;
	private static final int CONVEX = -1;

	// ===========================================================
	// Fields
	// ===========================================================

	private int mConcaveVertexCount;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public List<Vector2Copy> computeTriangles(final List<Vector2Copy> pVertices) {
		// TODO Check if LinkedList performs better
		final ArrayList<Vector2Copy> triangles = new ArrayList<Vector2Copy>();
		final ArrayList<Vector2Copy> vertices = new ArrayList<Vector2Copy>(pVertices.size());
		vertices.addAll(pVertices);

		if(vertices.size() == 3) {
			triangles.addAll(vertices);
			return triangles;
		}

		while(vertices.size() >= 3) {
			// TODO Usually(Always?) only the Types of the vertices next to the ear change! --> Improve
			final int vertexTypes[] = this.classifyVertices(vertices);

			final int vertexCount = vertices.size();
			for(int index = 0; index < vertexCount; index++) {
				if(this.isEarTip(vertices, index, vertexTypes)) {
					this.cutEarTip(vertices, index, triangles);
					break;
				}
			}
		}

		return triangles;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private static boolean areVerticesClockwise(final ArrayList<Vector2Copy> pVertices) {
		final int vertexCount = pVertices.size();

		float area = 0;
		for(int i = 0; i < vertexCount; i++) {
			final Vector2Copy p1 = pVertices.get(i);
			final Vector2Copy p2 = pVertices.get(EarClippingTriangulatorCopy.computeNextIndex(pVertices, i));
			area += p1.x * p2.y - p2.x * p1.y;
		}

		if(area < 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param pVertices
	 * @return An array of length <code>pVertices.size()</code> filled with either {@link EarClippingTriangulatorCopy#CONCAVE} or
	 * {@link EarClippingTriangulatorCopy#CONVEX}.
	 */
	private int[] classifyVertices(final ArrayList<Vector2Copy> pVertices) {
		final int vertexCount = pVertices.size();

		final int[] vertexTypes = new int[vertexCount];
		this.mConcaveVertexCount = 0;

		/* Ensure vertices are in clockwise order. */
		if(!EarClippingTriangulatorCopy.areVerticesClockwise(pVertices)) {
			Collections.reverse(pVertices);
		}

		for(int index = 0; index < vertexCount; index++) {
			final int previousIndex = EarClippingTriangulatorCopy.computePreviousIndex(pVertices, index);
			final int nextIndex = EarClippingTriangulatorCopy.computeNextIndex(pVertices, index);

			final Vector2Copy previousVertex = pVertices.get(previousIndex);
			final Vector2Copy currentVertex = pVertices.get(index);
			final Vector2Copy nextVertex = pVertices.get(nextIndex);

			if(EarClippingTriangulatorCopy.isTriangleConvex(previousVertex.x, previousVertex.y, currentVertex.x, currentVertex.y, nextVertex.x, nextVertex.y)) {
				vertexTypes[index] = CONVEX;
			} else {
				vertexTypes[index] = CONCAVE;
				this.mConcaveVertexCount++;
			}
		}

		return vertexTypes;
	}

	private static boolean isTriangleConvex(final float pX1, final float pY1, final float pX2, final float pY2, final float pX3, final float pY3) {
		if(EarClippingTriangulatorCopy.computeSpannedAreaSign(pX1, pY1, pX2, pY2, pX3, pY3) < 0) {
			return false;
		} else {
			return true;
		}
	}

	private static int computeSpannedAreaSign(final float pX1, final float pY1, final float pX2, final float pY2, final float pX3, final float pY3) {
		float area = 0;

		area += pX1 * (pY3 - pY2);
		area += pX2 * (pY1 - pY3);
		area += pX3 * (pY2 - pY1);

		return (int)Math.signum(area);
	}

	/**
	 * @return <code>true</code> when the Triangles contains one or more vertices, <code>false</code> otherwise.
	 */
	private static boolean isAnyVertexInTriangle(final ArrayList<Vector2Copy> pVertices, final int[] pVertexTypes, final float pX1, final float pY1, final float pX2, final float pY2, final float pX3, final float pY3) {
		int i = 0;

		final int vertexCount = pVertices.size();
		while(i < vertexCount - 1) {
			if((pVertexTypes[i] == CONCAVE)) {
				final Vector2Copy currentVertex = pVertices.get(i);

				final float currentVertexX = currentVertex.x;
				final float currentVertexY = currentVertex.y;

				/* TODO The following condition fails for perpendicular, axis aligned triangles! 
				 * Removing it doesn't seem to cause problems. 
				 * Maybe it was an optimization?
				 * Maybe it tried to handle collinear pieces ? */
//				if(((currentVertexX != pX1) && (currentVertexY != pY1)) || ((currentVertexX != pX2) && (currentVertexY != pY2)) || ((currentVertexX != pX3) && (currentVertexY != pY3))) {
					final int areaSign1 = EarClippingTriangulatorCopy.computeSpannedAreaSign(pX1, pY1, pX2, pY2, currentVertexX, currentVertexY);
					final int areaSign2 = EarClippingTriangulatorCopy.computeSpannedAreaSign(pX2, pY2, pX3, pY3, currentVertexX, currentVertexY);
					final int areaSign3 = EarClippingTriangulatorCopy.computeSpannedAreaSign(pX3, pY3, pX1, pY1, currentVertexX, currentVertexY);

					if(areaSign1 > 0 && areaSign2 > 0 && areaSign3 > 0) {
						return true;
					} else if(areaSign1 <= 0 && areaSign2 <= 0 && areaSign3 <= 0) {
						return true;
					}
//				}
			}
			i++;
		}
		return false;
	}

	private boolean isEarTip(final ArrayList<Vector2Copy> pVertices, final int pEarTipIndex, final int[] pVertexTypes) {
		if(this.mConcaveVertexCount != 0) {
			final Vector2Copy previousVertex = pVertices.get(EarClippingTriangulatorCopy.computePreviousIndex(pVertices, pEarTipIndex));
			final Vector2Copy currentVertex = pVertices.get(pEarTipIndex);
			final Vector2Copy nextVertex = pVertices.get(EarClippingTriangulatorCopy.computeNextIndex(pVertices, pEarTipIndex));

			if(EarClippingTriangulatorCopy.isAnyVertexInTriangle(pVertices, pVertexTypes, previousVertex.x, previousVertex.y, currentVertex.x, currentVertex.y, nextVertex.x, nextVertex.y)) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

	private void cutEarTip(final ArrayList<Vector2Copy> pVertices, final int pEarTipIndex, final ArrayList<Vector2Copy> pTriangles) {
		final int previousIndex = EarClippingTriangulatorCopy.computePreviousIndex(pVertices, pEarTipIndex);
		final int nextIndex = EarClippingTriangulatorCopy.computeNextIndex(pVertices, pEarTipIndex);

		if(!EarClippingTriangulatorCopy.isCollinear(pVertices, previousIndex, pEarTipIndex, nextIndex)) {
			pTriangles.add(new Vector2Copy(pVertices.get(previousIndex)));
			pTriangles.add(new Vector2Copy(pVertices.get(pEarTipIndex)));
			pTriangles.add(new Vector2Copy(pVertices.get(nextIndex)));
		}

		pVertices.remove(pEarTipIndex);
		if(pVertices.size() >= 3) {
			EarClippingTriangulatorCopy.removeCollinearNeighborEarsAfterRemovingEarTip(pVertices, pEarTipIndex);
		}
	}

	private static void removeCollinearNeighborEarsAfterRemovingEarTip(final ArrayList<Vector2Copy> pVertices, final int pEarTipCutIndex) {
		final int collinearityCheckNextIndex = pEarTipCutIndex % pVertices.size();
		int collinearCheckPreviousIndex = EarClippingTriangulatorCopy.computePreviousIndex(pVertices, collinearityCheckNextIndex);

		if(EarClippingTriangulatorCopy.isCollinear(pVertices, collinearityCheckNextIndex)) {
			pVertices.remove(collinearityCheckNextIndex);

			if(pVertices.size() > 3) {
				/* Update */
				collinearCheckPreviousIndex = EarClippingTriangulatorCopy.computePreviousIndex(pVertices, collinearityCheckNextIndex);
				if(EarClippingTriangulatorCopy.isCollinear(pVertices, collinearCheckPreviousIndex)){
					pVertices.remove(collinearCheckPreviousIndex);
				}
			}
		} else if(EarClippingTriangulatorCopy.isCollinear(pVertices, collinearCheckPreviousIndex)){
			pVertices.remove(collinearCheckPreviousIndex);
		}
	}

	private static boolean isCollinear(final ArrayList<Vector2Copy> pVertices, final int pIndex) {
		final int previousIndex = EarClippingTriangulatorCopy.computePreviousIndex(pVertices, pIndex);
		final int nextIndex = EarClippingTriangulatorCopy.computeNextIndex(pVertices, pIndex);

		return EarClippingTriangulatorCopy.isCollinear(pVertices, previousIndex, pIndex, nextIndex);
	}

	private static boolean isCollinear(final ArrayList<Vector2Copy> pVertices, final int pPreviousIndex, final int pIndex, final int pNextIndex) {
		final Vector2Copy previousVertex = pVertices.get(pPreviousIndex);
		final Vector2Copy vertex = pVertices.get(pIndex);
		final Vector2Copy nextVertex = pVertices.get(pNextIndex);

		return EarClippingTriangulatorCopy.computeSpannedAreaSign(previousVertex.x, previousVertex.y, vertex.x, vertex.y, nextVertex.x, nextVertex.y) == 0;
	}

	private static int computePreviousIndex(final List<Vector2Copy> pVertices, final int pIndex) {
		return pIndex == 0 ? pVertices.size() - 1 : pIndex - 1;
	}

	private static int computeNextIndex(final List<Vector2Copy> pVertices, final int pIndex) {
		return pIndex == pVertices.size() - 1 ? 0 : pIndex + 1;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
