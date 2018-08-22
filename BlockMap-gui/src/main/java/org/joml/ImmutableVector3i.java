/* (C) Copyright 2015-2017 Richard Greenlees
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE. */
package org.joml;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.joml.internal.MemUtil;
import org.joml.internal.Options;
import org.joml.internal.Runtime;

/**
 * Contains the definition of a Vector comprising 3 ints and associated transformations. The values of this class are set in the constructor and cannot be
 * changed. Operations that modify this vector either take in a destination vector that will be changed or return a changed immutable copy of this vector with
 * the computation's result.
 *
 * @author Richard Greenlees
 * @author Kai Burjack
 * @author Hans Uhlig
 * @author Julian Fischer
 */
public class ImmutableVector3i implements Vector3ic {

	/**
	 * The x component of the vector.
	 */
	public final int	x;
	/**
	 * The y component of the vector.
	 */
	public final int	y;
	/**
	 * The z component of the vector.
	 */
	public final int	z;

	/**
	 * Create a new {@link Vector3i} of <tt>(0, 0, 0)</tt>.
	 *
	 * @Deprecated Use {@link #ZERO} instead.
	 */
	@Deprecated
	public ImmutableVector3i() {
		this(0);
	}

	/**
	 * Create a new {@link Vector3i} and initialize all three components with the given value.
	 *
	 * @param d the value of all three components
	 */
	public ImmutableVector3i(int d) {
		this(d, d, d);
	}

	/**
	 * Create a new {@link Vector3i} with the given component values.
	 *
	 * @param x the value of x
	 * @param y the value of y
	 * @param z the value of z
	 */
	public ImmutableVector3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Create a new {@link Vector3i} with the same values as <code>v</code>.
	 *
	 * @param v the {@link Vector3ic} to copy the values from
	 */
	public ImmutableVector3i(Vector3ic v) {
		this.x = v.x();
		this.y = v.y();
		this.z = v.z();
	}

	/**
	 * Create a new {@link Vector3i} with the first two components from the given <code>v</code> and the given <code>z</code>
	 *
	 * @param v the {@link Vector2ic} to copy the values from
	 * @param z the z component
	 */
	public ImmutableVector3i(Vector2ic v, int z) {
		this.x = v.x();
		this.y = v.y();
		this.z = z;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#x() */
	@Override
	public int x() {
		return this.x;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#y() */
	@Override
	public int y() {
		return this.y;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#z() */
	@Override
	public int z() {
		return this.z;
	}

	/**
	 * Set the value of the specified component of this vector and returns it in a new vector.
	 *
	 * @param component the component whose value to set, within <tt>[0..2]</tt>
	 * @param value the value to set
	 * @return a new {@code ImmutableVector3i} containing a modified version of <code>this</code> according to the operation
	 * @throws IllegalArgumentException if <code>component</code> is not within <tt>[0..2]</tt>
	 */
	public ImmutableVector3i setComponent(int component, int value) throws IllegalArgumentException {
		int x = this.x;
		int y = this.y;
		int z = this.z;
		switch (component) {
			case 0:
				x = value;
				break;
			case 1:
				y = value;
				break;
			case 2:
				z = value;
				break;
			default:
				throw new IllegalArgumentException();
		}
		return new ImmutableVector3i(x, y, z);
	}

	// #ifdef __HAS_NIO__
	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#get(java.nio.IntBuffer) */
	@Override
	public IntBuffer get(IntBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#get(int, java.nio.IntBuffer) */
	@Override
	public IntBuffer get(int index, IntBuffer buffer) {
		MemUtil.INSTANCE.put(this.toMutableVector(), index, buffer);
		return buffer;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#get(java.nio.ByteBuffer) */
	@Override
	public ByteBuffer get(ByteBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#get(int, java.nio.ByteBuffer) */
	@Override
	public ByteBuffer get(int index, ByteBuffer buffer) {
		MemUtil.INSTANCE.put(this.toMutableVector(), index, buffer);
		return buffer;
	}
	// #endif

	public Vector3i toMutableVector() {
		return new Vector3i(this);
	}

	/**
	 * Subtract the supplied vector from this one and store the result in a new vector.
	 *
	 * @param v the vector to subtract
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i sub(Vector3ic v) {
		return new ImmutableVector3i(x - v.x(), y - v.y(), z - v.z());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#sub(org.joml.Vector3ic, org.joml.Vector3i) */
	@Override
	public Vector3i sub(Vector3ic v, Vector3i dest) {
		dest.x = x - v.x();
		dest.y = y - v.y();
		dest.z = z - v.z();
		return dest;
	}

	/**
	 * Decrement the components of this vector by the given values.
	 *
	 * @param x the x component to subtract
	 * @param y the y component to subtract
	 * @param z the z component to subtract
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i sub(int x, int y, int z) {
		return new ImmutableVector3i(this.x - x, this.y - y, this.z - z);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#sub(int, int, int, org.joml.Vector3i) */
	@Override
	public Vector3i sub(int x, int y, int z, Vector3i dest) {
		dest.x = this.x - x;
		dest.y = this.y - y;
		dest.z = this.z - z;
		return dest;
	}

	/**
	 * Add the supplied vector to this one.
	 *
	 * @param v the vector to add
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i add(Vector3ic v) {
		return add(v.x(), v.y(), v.z());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#add(org.joml.Vector3ic, org.joml.Vector3i) */
	@Override
	public Vector3i add(Vector3ic v, Vector3i dest) {
		dest.x = x + v.x();
		dest.y = y + v.y();
		dest.z = z + v.z();
		return dest;
	}

	/**
	 * Increment the components of this vector by the given values.
	 *
	 * @param x the x component to add
	 * @param y the y component to add
	 * @param z the z component to add
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i add(int x, int y, int z) {
		return new ImmutableVector3i(this.x + x, this.y + y, this.z + z);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#add(int, int, int, org.joml.Vector3i) */
	@Override
	public Vector3i add(int x, int y, int z, Vector3i dest) {
		dest.x = this.x + x;
		dest.y = this.y + y;
		dest.z = this.z + z;
		return dest;
	}

	/**
	 * Multiply all components of this vector by the given scalar value.
	 *
	 * @param scalar the scalar to multiply this vector by
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i mul(int scalar) {
		return new ImmutableVector3i(x * scalar, y * scalar, z * scalar);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#mul(int, org.joml.Vector3i) */
	@Override
	public Vector3i mul(int scalar, Vector3i dest) {
		dest.x = x * scalar;
		dest.y = y * scalar;
		dest.y = z * scalar;
		return dest;
	}

	/**
	 * Multiply the componentes of this vector by the values given in <code>v</code>.
	 *
	 * @param v the vector to multiply
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i mul(Vector3ic v) {
		return new ImmutableVector3i(this.x * v.x(), this.y * v.y(), this.z * v.z());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#mul(org.joml.Vector3ic, org.joml.Vector3i) */
	@Override
	public Vector3i mul(Vector3ic v, Vector3i dest) {
		dest.x = x * v.x();
		dest.y = y * v.y();
		dest.z = z * v.z();
		return dest;
	}

	/**
	 * Multiply the components of this vector by the given values.
	 *
	 * @param x the x component to multiply
	 * @param y the y component to multiply
	 * @param z the z component to multiply
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i mul(int x, int y, int z) {
		return new ImmutableVector3i(this.x * x, this.y * y, this.z * z);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#mul(int, int, int, org.joml.Vector3i) */
	@Override
	public Vector3i mul(int x, int y, int z, Vector3i dest) {
		dest.x = this.x * x;
		dest.y = this.y * y;
		dest.z = this.z * z;
		return dest;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#lengthSquared() */
	@Override
	public long lengthSquared() {
		return x * x + y * y + z * z;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#length() */
	@Override
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#distance(org.joml.Vector3ic) */
	@Override
	public double distance(Vector3ic v) {
		return Math.sqrt(distanceSquared(v));
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#distance(int, int, int) */
	@Override
	public double distance(int x, int y, int z) {
		return Math.sqrt(distanceSquared(x, y, z));
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#distanceSquared(org.joml.Vector3ic) */
	@Override
	public long distanceSquared(Vector3ic v) {
		int dx = this.x - v.x();
		int dy = this.y - v.y();
		int dz = this.z - v.z();
		return dx * dx + dy * dy + dz * dz;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#distanceSquared(int, int, int) */
	@Override
	public long distanceSquared(int x, int y, int z) {
		int dx = this.x - x;
		int dy = this.y - y;
		int dz = this.z - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public static final ImmutableVector3i ZERO = new ImmutableVector3i();

	/**
	 * Return a string representation of this vector.
	 * <p>
	 * This method creates a new {@link DecimalFormat} on every invocation with the format string "<tt>0.000E0;-</tt>".
	 *
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return Runtime.formatNumbers(toString(Options.NUMBER_FORMAT));
	}

	/**
	 * Return a string representation of this vector by formatting the vector components with the given {@link NumberFormat}.
	 *
	 * @param formatter the {@link NumberFormat} used to format the vector components with
	 * @return the string representation
	 */
	public String toString(NumberFormat formatter) {
		return "(" + formatter.format(x) + " " + formatter.format(y) + " " + formatter.format(z) + ")";
	}

	/**
	 * Negate this vector.
	 *
	 * @return a new {@code ImmutableVector3i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector3i negate() {
		return new ImmutableVector3i(-x, -y, -z);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector3ic#negate(org.joml.Vector3i) */
	@Override
	public Vector3i negate(Vector3i dest) {
		dest.x = -x;
		dest.y = -y;
		dest.z = -z;
		return dest;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Vector3i other = (Vector3i) obj;
		if (x != other.x) {
			return false;
		}
		if (y != other.y) {
			return false;
		}
		if (z != other.z) {
			return false;
		}
		return true;
	}

	@Override
	public Vector3ic getToAddress(long address) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Vector3i min(Vector3ic v, Vector3i dest) {
		dest.x = x < v.x() ? x : v.x();
		dest.y = y < v.y() ? y : v.y();
		dest.z = z < v.z() ? z : v.z();
		return dest;
	}

	@Override
	public Vector3i max(Vector3ic v, Vector3i dest) {
		dest.x = x > v.x() ? x : v.x();
		dest.y = y > v.y() ? y : v.y();
		dest.z = z > v.z() ? z : v.z();
		return dest;
	}

	@Override
	public int get(int component) throws IllegalArgumentException {
		switch (component) {
		case 0:
			return x;
		case 1:
			return y;
		case 2:
			return z;
		default:
			throw new IllegalArgumentException();
		}
	}
}