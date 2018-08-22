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

/**
 * Represents a 2D vector with single-precision. The values of this class are set in the constructor and cannot be changed. Operations that modify this vector
 * either take in a destination vector that will be changed or return a changed immutable copy of this vector with the computation's result.
 *
 * @author RGreenlees
 * @author Kai Burjack
 * @author Hans Uhlig
 * @author Julian Fischer
 */
public class ImmutableVector2i implements Vector2ic {

	/**
	 * The x component of the vector.
	 */
	public final int	x;
	/**
	 * The y component of the vector.
	 */
	public final int	y;

	/**
	 * Create a new {@link ImmutableVector2i} and initialize its components to zero.
	 *
	 * @Deprecated Use {@link #ZERO} instead.
	 */
	@Deprecated
	public ImmutableVector2i() {
		this(0);
	}

	/**
	 * Create a new {@link ImmutableVector2i} and initialize both of its components with the given value.
	 *
	 * @param s the value of both components
	 */
	public ImmutableVector2i(int s) {
		this(s, s);
	}

	/**
	 * Create a new {@link ImmutableVector2i} and initialize its components to the given values.
	 *
	 * @param x the x component
	 * @param y the y component
	 */
	public ImmutableVector2i(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Create a new {@link ImmutableVector2i} and initialize its components to the one of the given vector.
	 *
	 * @param v the {@link Vector2ic} to copy the values from
	 */
	public ImmutableVector2i(Vector2ic v) {
		x = v.x();
		y = v.y();
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#x() */
	@Override
	public int x() {
		return this.x;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#y() */
	@Override
	public int y() {
		return this.y;
	}

	/**
	 * Set the value of the specified component of this vector and returns it in a new vector.
	 *
	 * @param component the component whose value to set, within <tt>[0..1]</tt>
	 * @param value the value to set
	 * @return a new {@code ImmutableVector2i} containing a modified version of <code>this</code> according to the operation
	 * @throws IllegalArgumentException if <code>component</code> is not within <tt>[0..1]</tt>
	 */
	public ImmutableVector2i setComponent(int component, int value) throws IllegalArgumentException {
		int x = this.x;
		int y = this.y;
		switch (component) {
			case 0:
				x = value;
				break;
			case 1:
				y = value;
				break;
			default:
				throw new IllegalArgumentException();
		}
		return new ImmutableVector2i(x, y);
	}

	// #ifdef __HAS_NIO__
	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#get(java.nio.ByteBuffer) */
	@Override
	public ByteBuffer get(ByteBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#get(int, java.nio.ByteBuffer) */
	@Override
	public ByteBuffer get(int index, ByteBuffer buffer) {
		MemUtil.INSTANCE.put(this.toMutableVector(), index, buffer);
		return buffer;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#get(java.nio.IntBuffer) */
	@Override
	public IntBuffer get(IntBuffer buffer) {
		return get(buffer.position(), buffer);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#get(int, java.nio.IntBuffer) */
	@Override
	public IntBuffer get(int index, IntBuffer buffer) {
		MemUtil.INSTANCE.put(this.toMutableVector(), index, buffer);
		return buffer;
	}
	// #endif

	public Vector2i toMutableVector() {
		return new Vector2i(this);
	}

	/**
	 * Subtract the supplied vector from this one and store the result in a new vector.
	 *
	 * @param v the vector to subtract
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i sub(Vector2ic v) {
		return new ImmutableVector2i(x - v.x(), y - v.y());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#sub(org.joml.Vector2ic, org.joml.Vector2i) */
	@Override
	public Vector2i sub(Vector2ic v, Vector2i dest) {
		dest.x = x - v.x();
		dest.y = y - v.y();
		return dest;
	}

	/**
	 * Decrement the components of this vector by the given values.
	 *
	 * @param x the x component to subtract
	 * @param y the y component to subtract
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i sub(int x, int y) {
		return new ImmutableVector2i(this.x - x, this.y - y);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#sub(int, int, org.joml.Vector2i) */
	@Override
	public Vector2i sub(int x, int y, Vector2i dest) {
		dest.x = this.x - x;
		dest.y = this.y - y;
		return dest;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#lengthSquared() */
	@Override
	public long lengthSquared() {
		return x * x + y * y;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#length() */
	@Override
	public double length() {
		return Math.sqrt(lengthSquared());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#distance(org.joml.Vector2ic) */
	@Override
	public double distance(Vector2ic v) {
		return Math.sqrt(distanceSquared(v));
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#distance(int, int) */
	@Override
	public double distance(int x, int y) {
		return Math.sqrt(distanceSquared(x, y));
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#distanceSquared(org.joml.Vector2ic) */
	@Override
	public long distanceSquared(Vector2ic v) {
		int dx = this.x - v.x();
		int dy = this.y - v.y();
		return dx * dx + dy * dy;
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#distanceSquared(int, int) */
	@Override
	public long distanceSquared(int x, int y) {
		int dx = this.x - x;
		int dy = this.y - y;
		return dx * dx + dy * dy;
	}

	/**
	 * Add the supplied vector to this one.
	 *
	 * @param v the vector to add
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i add(Vector2ic v) {
		return add(v.x(), v.y());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#add(org.joml.Vector2ic, org.joml.Vector2i) */
	@Override
	public Vector2i add(Vector2ic v, Vector2i dest) {
		dest.x = x + v.x();
		dest.y = y + v.y();
		return dest;
	}

	/**
	 * Increment the components of this vector by the given values.
	 *
	 * @param x the x component to add
	 * @param y the y component to add
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i add(int x, int y) {
		return new ImmutableVector2i(this.x + x, this.y + y);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#add(int, int, int, org.joml.Vector2i) */
	@Override
	public Vector2i add(int x, int y, Vector2i dest) {
		dest.x = this.x + x;
		dest.y = this.y + y;
		return dest;
	}

	/**
	 * Multiply all components of this vector by the given scalar value.
	 *
	 * @param scalar the scalar to multiply this vector by
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i mul(int scalar) {
		return new ImmutableVector2i(x * scalar, y * scalar);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#mul(int, org.joml.Vector2i) */
	@Override
	public Vector2i mul(int scalar, Vector2i dest) {
		dest.x = x * scalar;
		dest.y = y * scalar;
		return dest;
	}

	/**
	 * Multiply the componentes of this vector by the values given in <code>v</code>.
	 *
	 * @param v the vector to multiply
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i mul(Vector2ic v) {
		return new ImmutableVector2i(this.x * v.x(), this.y * v.y());
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#mul(org.joml.Vector2ic, org.joml.Vector2i) */
	@Override
	public Vector2i mul(Vector2ic v, Vector2i dest) {
		dest.x = x * v.x();
		dest.y = y * v.y();
		return dest;
	}

	/**
	 * Multiply the components of this vector by the given values.
	 *
	 * @param x the x component to multiply
	 * @param y the y component to multiply
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i mul(int x, int y) {
		return new ImmutableVector2i(this.x * x, this.y * y);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#mul(int, int, int, org.joml.Vector2i) */
	@Override
	public Vector2i mul(int x, int y, Vector2i dest) {
		dest.x = this.x * x;
		dest.y = this.y * y;
		return dest;
	}

	public static final ImmutableVector2i ZERO = new ImmutableVector2i();

	/**
	 * Negate this vector.
	 *
	 * @return a new {@code ImmutableVector2i} containing a modified version of <tt>this</tt> according to the operation
	 */
	public ImmutableVector2i negate() {
		return new ImmutableVector2i(-x, -y);
	}

	/* (non-Javadoc)
	 *
	 * @see org.joml.Vector2ic#negate(org.joml.Vector2i) */
	@Override
	public Vector2i negate(Vector2i dest) {
		dest.x = -x;
		dest.y = -y;
		return dest;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
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
		ImmutableVector2i other = (ImmutableVector2i) obj;
		if (x != other.x) {
			return false;
		}
		if (y != other.y) {
			return false;
		}
		return true;
	}

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
		return "(" + formatter.format(x) + " " + formatter.format(y) + ")";
	}

}
