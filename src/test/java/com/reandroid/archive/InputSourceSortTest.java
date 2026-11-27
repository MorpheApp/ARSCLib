package com.reandroid.archive;

import org.junit.Assert;
import org.junit.Test;

public class InputSourceSortTest {
    @Test
    public void testSourcesWithSortAreOrderedBySort() {
        InputSource first = source("b.txt", 0);
        InputSource second = source("a.txt", 1);

        Assert.assertTrue(InputSource.ALIAS_COMPARATOR.compare(first, second) < 0);
        Assert.assertTrue(InputSource.ALIAS_COMPARATOR.compare(second, first) > 0);
    }

    @Test
    public void testSourceWithoutSortComesLast() {
        InputSource sorted = source("b.txt", 5);
        InputSource unsorted = source("a.txt", -1);

        Assert.assertTrue(InputSource.ALIAS_COMPARATOR.compare(sorted, unsorted) < 0);
        Assert.assertTrue(InputSource.ALIAS_COMPARATOR.compare(unsorted, sorted) > 0);
    }

    @Test
    public void testSourcesWithoutSortAreOrderedByAlias() {
        InputSource manifest = source("AndroidManifest.xml", -1);
        InputSource asset = source("assets/a.txt", -1);

        Assert.assertTrue(InputSource.ALIAS_COMPARATOR.compare(manifest, asset) < 0);
        Assert.assertTrue(InputSource.ALIAS_COMPARATOR.compare(asset, manifest) > 0);
    }

    private static InputSource source(String name, int sort) {
        InputSource source = new ByteInputSource(new byte[0], name);
        source.setSort(sort);
        return source;
    }
}
