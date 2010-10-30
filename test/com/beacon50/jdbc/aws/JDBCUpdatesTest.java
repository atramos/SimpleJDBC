package com.beacon50.jdbc.aws;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class JDBCUpdatesTest {
    private static AmazonSimpleDB sdb;
    private static String domain;


    @Test
    public void testUpdate() throws Exception {
        Connection conn = getConnection();
        Statement st = conn.createStatement();
        String update = "UPDATE users SET age = 45 where name = 'Joe Smith'";
        int val = st.executeUpdate(update);
        assertEquals("val should be 1", 1, val);
        Thread.sleep(2000);
    }

    @BeforeClass
    public static void initialize() throws Exception {
        sdb = new AmazonSimpleDBClient(
                new BasicAWSCredentials(System.getProperty("accessKey"),
                        System.getProperty("secretKey")));
        domain = "users";
        sdb.createDomain(new CreateDomainRequest(domain));

        List<ReplaceableItem> data = new ArrayList<ReplaceableItem>();

        data.add(new ReplaceableItem().withName("user_01").withAttributes(
                new ReplaceableAttribute().withName("name").withValue("Joe Smith"),
                new ReplaceableAttribute().withName("age").withValue("34")));

        sdb.batchPutAttributes(new BatchPutAttributesRequest(domain, data));
        Thread.sleep(2000);
    }

    public static Connection getConnection() throws Exception {
        Connection con = null;
        Properties prop = new Properties();
        prop.setProperty("secretKey", System.getProperty("secretKey"));
        prop.setProperty("accessKey", System.getProperty("accessKey"));
        Class.forName("com.beacon50.jdbc.aws.SimpleDBDriver");
        return DriverManager.getConnection("jdbc:simpledb://sdb.amazonaws.com", prop);
    }

    @AfterClass
    public static void deInitialize() throws Exception {
        Thread.sleep(2000);
        String qry = "select * from `users` where name = 'Joe Smith'";
        SelectRequest selectRequest = new SelectRequest(qry);
        boolean itemFound = false;
        for (Item item : sdb.select(selectRequest).getItems()) {
            itemFound = true;
            List<Attribute> attrs = item.getAttributes();
            for (Attribute attr : attrs) {
                if (attr.getName().equals("name")) {
                    assertEquals("name wasn't Joe Smith", "Joe Smith", attr.getValue());
                } else {
                    //note! values are encoded! 
                    assertEquals("name wasn't 00045", "00045", attr.getValue());
                }
            }
        }
        if (!itemFound) {
            fail("item wasn't found?");
        }


        sdb.deleteAttributes(new DeleteAttributesRequest(domain, "user_01"));
        sdb.deleteDomain(new DeleteDomainRequest(domain));

    }
}