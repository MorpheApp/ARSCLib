package com.reandroid.arsc.chunk;

import com.reandroid.TestUtils;
import com.reandroid.apk.framework.InternalFrameworks;
import com.reandroid.arsc.array.ResValueMapArray;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlDocument;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.header.TypeHeader;
import com.reandroid.arsc.io.BlockReader;
import com.reandroid.arsc.model.FrameworkTable;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.AttributeDataFormat;
import com.reandroid.arsc.value.AttributeType;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResValueMap;
import com.reandroid.arsc.value.ValueItem;
import com.reandroid.utils.HexUtil;
import com.reandroid.utils.collection.CollectionUtil;
import com.reandroid.xml.XMLFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class PackageBlockTest {

    @Test
    public void testNameAndId() {
        TableBlock tableBlock = new TableBlock();

        PackageBlock packageBlock = tableBlock.newPackage(
                TestUtils.MAIN_PACKAGE_ID, TestUtils.MAIN_PACKAGE_NAME);
        Assert.assertEquals("Packages count", 1, tableBlock.size());

        Assert.assertEquals("Package id", TestUtils.MAIN_PACKAGE_ID, packageBlock.getId());
        Assert.assertEquals("Package name", TestUtils.MAIN_PACKAGE_NAME, packageBlock.getName());
        Assert.assertNotNull(tableBlock.get(0));

        Assert.assertNotNull(tableBlock.getPackageBlockById(TestUtils.MAIN_PACKAGE_ID));
        Assert.assertTrue(tableBlock.getPackages(TestUtils.MAIN_PACKAGE_NAME).hasNext());
    }

    @Test
    public void testAttributeAndNamespaceOnMultiPackage() {
        TableBlock tableBlock = new TableBlock();

        PackageBlock packageMain = tableBlock.newPackage(
                TestUtils.MAIN_PACKAGE_ID, TestUtils.MAIN_PACKAGE_NAME);
        Entry idEntry = packageMain.getOrCreate("", "id", "id_main");
        idEntry.setDataAsBoolean(true);

        Entry attr1 = createNameAttribute(tableBlock.newPackage(
                TestUtils.MAIN_PACKAGE_ID + 1, TestUtils.MAIN_PACKAGE_NAME + ".app1"));
        Entry attr2 = createNameAttribute(tableBlock.newPackage(
                TestUtils.MAIN_PACKAGE_ID + 2, TestUtils.MAIN_PACKAGE_NAME + ".app2"));
        Entry attr3 = createNameAttribute(tableBlock.newPackage(
                TestUtils.MAIN_PACKAGE_ID + 3, TestUtils.MAIN_PACKAGE_NAME + ".app3"));

        FrameworkTable frameworkTable = InternalFrameworks.INSTANCE.getLatest().getTableBlock();
        ResourceEntry attrAndroidText = frameworkTable.getResource(
                "android", "attr", "text");

        ResXmlDocument document = new ResXmlDocument();
        ResXmlElement element = document.getDocumentElement();
        element.setName("TextView");

        ResXmlAttribute attribute = element.newAttribute();
        attribute.setName("",
                frameworkTable.getResource("android", "attr", "id").getResourceId());
        attribute.setDataAsResourceId(idEntry.getResourceId());

        attribute = element.newAttribute();
        attribute.setName("", attrAndroidText.getResourceId());
        attribute.setDataAsString("text-android-value");

        attribute = element.newAttribute();
        attribute.setName("", attr1.getResourceId());
        attribute.setDataAsString("text-app1-value");

        attribute = element.newAttribute();
        attribute.setName("", attr2.getResourceId());
        attribute.setDataAsString("text-app2-value");

        attribute = element.newAttribute();
        attribute.setName("", attr3.getResourceId());
        attribute.setDataAsString("text-app3-value");

        tableBlock.addFramework(frameworkTable);
        document.setPackageBlock(packageMain);

        document.fixNamespaces();
        document.fixAttributeNames();
        document.refreshFull();
        String expected = "<?xml version='1.0' encoding='utf-8' ?>\n" +
                "<TextView android:id=\"@id/id_main\"\n" +
                "          android:text=\"text-android-value\"\n" +
                "          app1:text=\"text-app1-value\"\n" +
                "          app2:text=\"text-app2-value\"\n" +
                "          app3:text=\"text-app3-value\"" +
                " xmlns:android=\"http://schemas.android.com/apk/res/android\"" +
                " xmlns:app1=\"http://schemas.android.com/apk/res/app1\"" +
                " xmlns:app2=\"http://schemas.android.com/apk/res/app2\"" +
                " xmlns:app3=\"http://schemas.android.com/apk/res/app3\" />";

        Assert.assertEquals(
                TestUtils.squeezeWhiteSpaces(expected),
                TestUtils.squeezeWhiteSpaces(document.toXmlString()));
    }
    private Entry createNameAttribute(PackageBlock packageBlock) {
        Entry attr = packageBlock.getOrCreate("", "attr", "text");
        attr.ensureComplex(true);
        attr.getResTableMapEntry().getHeader().setPublic(true);

        ResValueMapArray array = attr.getResValueMapArray();

        ResValueMap valueMap = array.createNext();
        valueMap.setAttributeType(AttributeType.FORMATS);

        valueMap.addAttributeTypeFormats(
                AttributeDataFormat.REFERENCE,
                AttributeDataFormat.STRING);

        array.refresh();
        return attr;
    }

    @Test
    public void testParsePublicXml() {
        TableBlock tableBlock = new TableBlock();

        PackageBlock packageBlock = tableBlock.newPackage(
                TestUtils.MAIN_PACKAGE_ID, TestUtils.MAIN_PACKAGE_NAME);

        String pubXml = "<?xml version='1.0' encoding='utf-8' ?>\n" +
                "<resources package=\"" + TestUtils.MAIN_PACKAGE_NAME
                + "\" id=\"0x" + HexUtil.toHex2(TestUtils.MAIN_PACKAGE_ID) + "\">\n" +

                "  <public id=\"0x7f010000\" type=\"anim\" name=\"name_0\" />\n" +
                "  <public id=\"0x7f010001\" type=\"anim\" name=\"name_1\" />\n" +
                "  <public id=\"0x7f010002\" type=\"anim\" name=\"name_2\" />\n" +
                "  <public id=\"0x7f010003\" type=\"anim\" name=\"name_3\" />\n" +


                "  <public id=\"0x7f020000\" type=\"attr\" name=\"name_0\" />\n" +
                "  <public id=\"0x7f020001\" type=\"attr\" name=\"name_1\" />\n" +
                "  <public id=\"0x7f020002\" type=\"attr\" name=\"name_2\" />\n" +
                "  <public id=\"0x7f020003\" type=\"attr\" name=\"name_3\" />\n" +

                "</resources>";

        Exception exception;
        try {
            packageBlock.parsePublicXml(XMLFactory.newPullParser(pubXml));
            exception = null;
        } catch (Exception e) {
            exception = e;
        }
        Assert.assertNull("Exception parsing public.xml: ", exception);

        Assert.assertEquals("Package name: ", TestUtils.MAIN_PACKAGE_NAME, packageBlock.getName());
        Assert.assertEquals("Resources types count: ", 2,
                packageBlock.getSpecTypePairArray().size());

        Assert.assertEquals("Resources count: ", 8,
                CollectionUtil.count(packageBlock.getResources()));

        ResourceEntry entryAnimName0 = packageBlock.getResource("anim", "name_0");
        Assert.assertNotNull("Null resource entry anim/name_0", entryAnimName0);
        Assert.assertEquals("Resource id anim/name_0: ", 0x7f010000, entryAnimName0.getResourceId());
        Assert.assertEquals("Resource type anim/name_0: ", "anim", entryAnimName0.getType());

        ResourceEntry entryAttrName3 = packageBlock.getResource("attr", "name_3");
        Assert.assertNotNull("Null resource entry anim/name_3", entryAttrName3);
        Assert.assertEquals("Resource id attr/name_3: ", 0x7f020003, entryAttrName3.getResourceId());
        Assert.assertEquals("Resource type attr/name_3: ", "attr", entryAttrName3.getType());
    }

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
        entry2.setDataAsString("second");

        Entry entry5 = typeBlock.getOrCreateEntry(5);
        entry5.setName("five", true);
        entry5.setDataAsString("fifth");

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

        List<ValueItem> values = CollectionUtil.toList(tableBlock.visitValues());
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
        Assert.assertEquals("second", reloadedType.getEntry(2).getResValue().getDataAsString());
        Assert.assertNull(reloadedType.getEntry(3));
        Assert.assertNull(reloadedType.getEntry(4));
        Assert.assertEquals("fifth", reloadedType.getEntry(5).getResValue().getDataAsString());
    }

    @Test
    public void testSparseAndDenseOffsetConversions() {
        TableBlock tableBlock = new TableBlock();
        PackageBlock packageBlock = tableBlock.newPackage(0x7f, "com.example.app");
        TypeBlock typeBlock = packageBlock.getOrCreateTypeBlock((byte) 1, "");
        typeBlock.setTypeName("string");

        Entry entry2 = typeBlock.getOrCreateEntry(2);
        entry2.setName("two", true);
        entry2.setDataAsString("second");

        Entry entry5 = typeBlock.getOrCreateEntry(5);
        entry5.setName("five", true);
        entry5.setDataAsString("fifth");

        tableBlock.refresh();

        typeBlock.getEntryArray().setOffsetType(TypeHeader.OFFSET_SPARSE);
        Assert.assertTrue(typeBlock.isSparse());
        Assert.assertEquals(2, typeBlock.size());
        Assert.assertEquals("second", typeBlock.getEntry(2).getResValue().getDataAsString());
        Assert.assertEquals("fifth", typeBlock.getEntry(5).getResValue().getDataAsString());
        Assert.assertNull(typeBlock.getEntry(0));
        Assert.assertNull(typeBlock.getEntry(3));

        typeBlock.getEntryArray().setOffsetType(TypeHeader.OFFSET_16);
        Assert.assertFalse(typeBlock.isSparse());
        Assert.assertTrue(typeBlock.isOffset16());
        Assert.assertEquals(6, typeBlock.size());
        Assert.assertEquals(2, typeBlock.realSize());
        Assert.assertNull(typeBlock.getEntry(0));
        Assert.assertNull(typeBlock.getEntry(1));
        Assert.assertEquals("second", typeBlock.getEntry(2).getResValue().getDataAsString());
        Assert.assertNull(typeBlock.getEntry(3));
        Assert.assertNull(typeBlock.getEntry(4));
        Assert.assertEquals("fifth", typeBlock.getEntry(5).getResValue().getDataAsString());

        typeBlock.getEntryArray().setOffsetType(TypeHeader.OFFSET_32);
        Assert.assertFalse(typeBlock.isSparse());
        Assert.assertFalse(typeBlock.isOffset16());
        Assert.assertEquals(6, typeBlock.size());
        Assert.assertEquals(2, typeBlock.realSize());
        Assert.assertNull(typeBlock.getEntry(0));
        Assert.assertEquals("second", typeBlock.getEntry(2).getResValue().getDataAsString());
        Assert.assertEquals("fifth", typeBlock.getEntry(5).getResValue().getDataAsString());
    }

}
