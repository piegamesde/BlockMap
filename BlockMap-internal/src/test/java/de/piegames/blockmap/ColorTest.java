package de.piegames.blockmap;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import de.piegames.blockmap.color.Color;

public class ColorTest {

	Random random;

	@Before
	public void resetRandom() {
		random = new Random(123456789);
	}

	private Color randomColor() {
		return new Color(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat());
	}

	@Test
	public void testAlphaBlending() {
		for (int i = 0; i < 10000; i++) {
			Color a = Color.TRANSPARENT, b = Color.TRANSPARENT;
			Color c = Color.TRANSPARENT, d = Color.TRANSPARENT;
			for (int j = 0; j < 100; j++) {
				Color random = randomColor();
				a = Color.alphaOver(a, random);
				b = Color.alphaUnder(random, b);
				assertColorEquals("Test " + i + ", iteration " + j, a, b);
				c = Color.alphaOver(random, c);
				d = Color.alphaUnder(d, random);
				assertColorEquals("Test " + i + ", iteration " + j, c, d);
			}
		}
	}

	@Test
	public void testMultipleAlphaOver() {
		for (int i = 0; i < 10000; i++) {
			Color a = Color.TRANSPARENT, b = Color.TRANSPARENT;
			for (int j = 0; j < 50; j++) {
				int times = random.nextInt(100);
				Color random = randomColor();
				for (int k = 0; k < times; k++)
					a = Color.alphaOver(a, random);
				b = Color.alphaOver(b, random, times);
				assertColorEquals("Test " + i + ", iteration " + j, a, b);
			}
		}
	}

	@Test
	public void testMultipleAlphaUnder() {
		for (int i = 0; i < 10000; i++) {
			Color a = Color.TRANSPARENT, b = Color.TRANSPARENT;
			for (int j = 0; j < 50; j++) {
				int times = random.nextInt(100);
				Color random = randomColor();
				for (int k = 0; k < times; k++)
					a = Color.alphaUnder(a, random);
				b = Color.alphaUnder(b, random, times);
				assertColorEquals("Test " + i + ", iteration " + j, a, b);
			}
		}
	}

	private static void assertColorEquals(String message, Color a, Color b) {
		assertEquals(message + ", a", a.a, b.a, 0.00001);
		assertEquals(message + ", r", a.r, b.r, 0.00001);
		assertEquals(message + ", g", a.g, b.g, 0.00001);
		assertEquals(message + ", b", a.b, b.b, 0.00001);
	}
}