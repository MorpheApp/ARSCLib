package com.reandroid.arsc.base;

import org.junit.Assert;
import org.junit.Test;

public class BlockTest {

    @Test
    public void testEqualsIgnoreTrailZero() {
        byte[] bytes = {1, 2, 3};
        Assert.assertTrue(Block.equalsIgnoreTrailZero(bytes, bytes));
        Assert.assertTrue(Block.equalsIgnoreTrailZero(bytes, new byte[]{1, 2, 3}));
        Assert.assertTrue(Block.equalsIgnoreTrailZero(bytes, new byte[]{1, 2, 3, 0, 0}));
        Assert.assertTrue(Block.equalsIgnoreTrailZero(new byte[]{1, 2, 3, 0}, bytes));
        Assert.assertTrue(Block.equalsIgnoreTrailZero(new byte[]{1, 0, 3, 0}, new byte[]{1, 0, 3}));
        Assert.assertTrue(Block.equalsIgnoreTrailZero(new byte[]{0}, new byte[0]));

        Assert.assertFalse(Block.equalsIgnoreTrailZero(bytes, new byte[]{1, 2}));
        Assert.assertFalse(Block.equalsIgnoreTrailZero(bytes, new byte[]{1, 2, 3, 0, 4}));
        Assert.assertFalse(Block.equalsIgnoreTrailZero(new byte[]{1, 2, 4}, bytes));
        Assert.assertFalse(Block.equalsIgnoreTrailZero(new byte[0], new byte[]{0, 1}));
        Assert.assertFalse(Block.equalsIgnoreTrailZero(new byte[]{0, 1}, new byte[]{1}));
    }

    @Test
    public void testHashIgnoreTrailZeros() {
        byte[] bytes = {1, 0, 3};
        int hash = Block.hashIgnoreTrailZeros(bytes);
        Assert.assertEquals(hash, Block.hashIgnoreTrailZeros(new byte[]{1, 0, 3, 0, 0}));
        Assert.assertEquals(Block.hashIgnoreTrailZeros(new byte[0]), Block.hashIgnoreTrailZeros(new byte[]{0, 0}));
        Assert.assertNotEquals(hash, Block.hashIgnoreTrailZeros(new byte[]{1, 0, 3, 0, 4}));
    }
}
