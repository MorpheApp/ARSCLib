package com.reandroid.arsc.chunk;

import com.reandroid.arsc.header.TypeHeader;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ValueItem;
import com.reandroid.utils.collection.CollectionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class PackageBlockTest {

    @Test
    public void testIsResourceId() {
        Assert.assertFalse(PackageBlock.isResourceId(0x7f00ffff));
        Assert.assertFalse(PackageBlock.isResourceId(0x00ff0000));
        Assert.assertFalse(PackageBlock.isResourceId(0x00ffffff));
        Assert.assertFalse(PackageBlock.isResourceId(0x0000ffff));
        Assert.assertFalse(PackageBlock.isResourceId(0xff000000));
        Assert.assertFalse(PackageBlock.isResourceId(0x0));
        Assert.assertTrue(PackageBlock.isResourceId(0x0101ffff));
        Assert.assertTrue(PackageBlock.isResourceId(0x01010000));
    }

    @Test
    public void testIsPackageId() {
        Assert.assertFalse(PackageBlock.isPackageId(0x00));
        Assert.assertFalse(PackageBlock.isPackageId(0xfff));
        Assert.assertFalse(PackageBlock.isPackageId(0xffff));
        Assert.assertFalse(PackageBlock.isPackageId(0xfffff));
        Assert.assertFalse(PackageBlock.isPackageId(0xffffff));
        Assert.assertFalse(PackageBlock.isPackageId(0xffffffff));
        Assert.assertTrue(PackageBlock.isPackageId(0x01));
        Assert.assertTrue(PackageBlock.isPackageId(0x11));
        Assert.assertTrue(PackageBlock.isPackageId(0xff));
    }

    @Test
    public void testDenseTypeBlockWithMissingEntries() throws IOException {
        TableBlock tableBlock = new TableBlock();
        PackageBlock packageBlock = tableBlock.newPackage(0x7f, "com.example.app");
        TypeBlock typeBlock = packageBlock.getOrCreateTypeBlock((byte) 1, "");
        typeBlock.setTypeName("string");

        Entry entry2 = typeBlock.getOrCreateEntry(2);
        entry2.setName("two", true);
        entry2.setValueAsString("second");

        Entry entry5 = typeBlock.getOrCreateEntry(5);
        entry5.setName("five", true);
        entry5.setValueAsString("fifth");

        tableBlock.refresh();

        Assert.assertEquals(6, typeBlock.size());
        Assert.assertEquals(2, typeBlock.realSize());
        Assert.assertNull(typeBlock.getEntry(0));
        Assert.assertNull(typeBlock.getEntry(1));
        Assert.assertNotNull(typeBlock.getEntry(2));
        Assert.assertNull(typeBlock.getEntry(3));
        Assert.assertNull(typeBlock.getEntry(4));
        Assert.assertNotNull(typeBlock.getEntry(5));

        List<Entry> iterEntries = CollectionUtil.toList(typeBlock.iterator());
        Assert.assertEquals(2, iterEntries.size());
        Assert.assertEquals(2, typeBlock.listEntries(false).size());
        Assert.assertEquals(2, typeBlock.listEntries(true).size());

        List<ValueItem> values = CollectionUtil.toList(tableBlock.allValues());
        Assert.assertEquals(2, values.size());

        byte[] bytes = tableBlock.getBytes();
        TableBlock reloaded = new TableBlock();
        reloaded.readBytes(new BlockReader(bytes));
        TypeBlock reloadedType = reloaded.getPackageBlockById(0x7f)
                .getSpecTypePair("string")
                .getTypeBlock("");

        Assert.assertEquals(6, reloadedType.size());
        Assert.assertEquals(2, reloadedType.realSize());
        Assert.assertNull(reloadedType.getEntry(0));
        Assert.assertNull(reloadedType.getEntry(1));
        Assert.assertEquals("second", reloadedType.getEntry(2).getResValue().getValueAsString());
        Assert.assertNull(reloadedType.getEntry(3));
        Assert.assertNull(reloadedType.getEntry(4));
        Assert.assertEquals("fifth", reloadedType.getEntry(5).getResValue().getValueAsString());
    }

    @Test
    public void testSparseAndDenseOffsetConversions() {
        TableBlock tableBlock = new TableBlock();
        PackageBlock packageBlock = tableBlock.newPackage(0x7f, "com.example.app");
        TypeBlock typeBlock = packageBlock.getOrCreateTypeBlock((byte) 1, "");
        typeBlock.setTypeName("string");

        Entry entry2 = typeBlock.getOrCreateEntry(2);
        entry2.setName("two", true);
        entry2.setValueAsString("second");

        Entry entry5 = typeBlock.getOrCreateEntry(5);
        entry5.setName("five", true);
        entry5.setValueAsString("fifth");

        tableBlock.refresh();

        typeBlock.getEntryArray().setOffsetType(TypeHeader.OFFSET_SPARSE);
        Assert.assertTrue(typeBlock.isSparse());
        Assert.assertEquals(2, typeBlock.size());
        Assert.assertEquals("second", typeBlock.getEntry(2).getResValue().getValueAsString());
        Assert.assertEquals("fifth", typeBlock.getEntry(5).getResValue().getValueAsString());
        Assert.assertNull(typeBlock.getEntry(0));
        Assert.assertNull(typeBlock.getEntry(3));

        typeBlock.getEntryArray().setOffsetType(TypeHeader.OFFSET_16);
        Assert.assertFalse(typeBlock.isSparse());
        Assert.assertTrue(typeBlock.isOffset16());
        Assert.assertEquals(6, typeBlock.size());
        Assert.assertEquals(2, typeBlock.realSize());
        Assert.assertNull(typeBlock.getEntry(0));
        Assert.assertNull(typeBlock.getEntry(1));
        Assert.assertEquals("second", typeBlock.getEntry(2).getResValue().getValueAsString());
        Assert.assertNull(typeBlock.getEntry(3));
        Assert.assertNull(typeBlock.getEntry(4));
        Assert.assertEquals("fifth", typeBlock.getEntry(5).getResValue().getValueAsString());

        typeBlock.getEntryArray().setOffsetType(TypeHeader.OFFSET_32);
        Assert.assertFalse(typeBlock.isSparse());
        Assert.assertFalse(typeBlock.isOffset16());
        Assert.assertEquals(6, typeBlock.size());
        Assert.assertEquals(2, typeBlock.realSize());
        Assert.assertNull(typeBlock.getEntry(0));
        Assert.assertEquals("second", typeBlock.getEntry(2).getResValue().getValueAsString());
        Assert.assertEquals("fifth", typeBlock.getEntry(5).getResValue().getValueAsString());
    }

}
