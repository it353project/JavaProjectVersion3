/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import model.SearchBean;
import model.ViewBean;

/**
 *
 * @author it3530219
 */
public class SearchDAOImpl implements SearchDAO {
    /* Performs a search against the database, using the searchBean's variables. */
    @Override
    public ArrayList searchRequest(SearchBean aSearch) {
        /* Takes in the SearchBean, builds query. Depending on what the user has 
        filled in for search criteria, the query will change. Each field will 
        add an AND condition to the select statement, as each one should narrow
        the possible parameters. */
        
        String[] stringName;
        String[] keywordHolder;
        String keywords = "";
        String firstName = "";
        String lastName = "";
        
        String accountNameSearch = "";
        String keywordSearch = "";
        String query = "";
        
        int clauseCounter = 0;
        
        /* Format Name into firstName, lastName, if AuthorName was used as a search criteria.
        The name field that's passed by SearchBean should be first parsed apart, first by ",",
        then by " ". If a comma is found, we'll assume that they've put lastname first, like 
        "Doe, John". If no comma is found, but a space is found, we'll assume "John Doe". If no
        spaces or commas are found, we will assume it's a lastname, and omit firstname from the search.*/
        if(!aSearch.getAuthorName().equals("")){
            stringName = aSearch.getAuthorName().split(",", 2);
            if (stringName.length == 1){
                stringName = aSearch.getAuthorName().split(" ", 2);
                if(stringName.length == 1){
                    lastName = aSearch.getAuthorName();
                    firstName = null;
                    accountNameSearch = "IT353.ACCOUNT.LASTNAME LIKE '%" +
                            lastName +"%'";
                }else{
                    firstName = stringName[0];
                    lastName = stringName[1];
                    accountNameSearch = "IT353.ACCOUNT.FIRSTNAME LIKE '%" +
                            firstName + "%' AND IT353.ACCOUNT.LASTNAME LIKE '%" 
                            + lastName +"%'";
                }
            }else{
                lastName = stringName[0];
                firstName = stringName[1];
                accountNameSearch = "IT353.ACCOUNT.FIRSTNAME LIKE '%" + 
                        firstName + "%' AND IT353.ACCOUNT.LASTNAME LIKE '%" +
                        lastName +"%'";
            }
        }
        
        /* Disassemble keywords, insert "'"s, reassemble, if there are keywords */
        if(aSearch.getKeywords().length() != 0){
            keywordHolder = aSearch.getKeywords().split(",");
            for(int i=0; i < keywordHolder.length; i++){
                if (i == keywordHolder.length-1){
                    /* If it's the last one, no comma at end. */
                    keywords = keywords + "'" + keywordHolder[i] + "'";
                }else{
                    keywords = keywords + "'" + keywordHolder[i] + "',";
                }        
            }
            keywordSearch = "IT353.KEYWORD.KEYWORD IN (" + keywords + ")";
        }
          
        /* Build the basic template for */
        String baseSelect = "SELECT"
                + " IT353.ACCOUNT.FIRSTNAME || ' ' || IT353.ACCOUNT.LASTNAME AS AUTHORNAME,"
                + " IT353.THESIS.THESISID, "
                + " IT353.THESIS.THESISNAME, "
                + " IT353.THESIS.COURSENO, "
                + " IT353.THESIS.ABSTRACT, "
                + " IT353.THESIS.ATTACHMENTLINK, "
                + " IT353.THESIS.SCREENCASTLINK, "
                + " IT353.THESIS.LIVELINK, "
                + " IT353.THESIS.UPLOADDATE, "
                + " IT353.KEYWORD.KEYWORD ";
        String fromClause = "FROM IT353.ACCOUNT ";
        String joinClause = "JOIN IT353.THESIS ON IT353.THESIS.ACCOUNTID = IT353.ACCOUNT.ACCOUNTID " +
            "JOIN IT353.KEYASSIGN ON IT353.KEYASSIGN.THESISID = IT353.THESIS.THESISID " +
            "JOIN IT353.KEYWORD ON IT353.KEYWORD.KEYWORDID = IT353.KEYASSIGN.KEYWORDID ";
           
        String whereClause = "WHERE ";
        
        /* Add author name to the WHERE clause if we have one */
        if(!accountNameSearch.isEmpty()){
            whereClause += accountNameSearch;
            clauseCounter++;
        }
        
        /* Adds the keyword search, if we've got one.If there has already been 
        an addition to the WHERE clause, tack on an AND in front of this one. */
        if(!keywordSearch.equals("") && clauseCounter > 0){
            whereClause += keywordSearch;
            clauseCounter++;
        }else if(!keywordSearch.equals("") && clauseCounter == 0){
            whereClause += " AND " + keywordSearch;
            clauseCounter++;
        }

        /* Adds the course number, if we've got one. If there has already been 
        an addition to the WHERE clause, tack on an AND in front of this one. */
        if (!aSearch.getCourseNo().isEmpty() && clauseCounter == 0){
            whereClause += "IT353.THESIS.COURSENO = " + aSearch.getCourseNo();
            clauseCounter++;
        }else if(!aSearch.getCourseNo().isEmpty()){
            whereClause += " AND IT353.THESIS.COURSENO = " + aSearch.getCourseNo();
            clauseCounter++;
        }
        
        /* Adds the date range, if we've got one. If there has already been 
        an addition to the WHERE clause, tack on an AND in front of this one. */
        if(aSearch.getStartDate() != null && clauseCounter > 0){
            whereClause += " AND IT353.THESIS.UPLOADDATE >= " + aSearch.getStartDate();
            clauseCounter++;            
        }else if(aSearch.getStartDate() != null){
            whereClause += "IT353.THESIS.UPLOADDATE >= " + aSearch.getStartDate();
            clauseCounter++;            
        }
        
        if(aSearch.getEndDate() != null && clauseCounter > 0){
            whereClause += " AND IT353.THESIS.UPLOADDATE >= " + aSearch.getEndDate();
            clauseCounter++;            
        }else if(aSearch.getEndDate() != null){
            whereClause += "IT353.THESIS.UPLOADDATE >= " + aSearch.getEndDate();
            clauseCounter++;            
        }
        
        String orderByClause = "ORDER BY IT353.THESIS.THESISID ASC";
        
        query = baseSelect + fromClause + joinClause + whereClause + orderByClause;
        /* Pass the now-built query on to the database. */
        ArrayList searchResults = performSearch(query);
        
        /* Return the search results */
        return searchResults;
    }

    public ArrayList performSearch(String query){
            ArrayList viewCollection = new ArrayList(); 

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
        
        try {         
            String myDB = "jdbc:derby://localhost:1527/IT353";  
            Connection DBConn = DriverManager.getConnection(myDB, "itkstu", "student");
            Statement stmt = DBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(query);
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
            ViewBean aView;
            String[] keywords = new String[20];
            int nextCounter = 0;
            Boolean EOF = false;
            int keywordCounter = 0;
            
            int thesisID = 0;
            
            while (rs.next()) {
                aView = new ViewBean();
                    keywordCounter = 0;
                    nextCounter = 0;
                    
                    thesisID = Integer.parseInt(rs.getString("THESISID"));
                    aView.setThesisID(Integer.parseInt(rs.getString("THESISID")));
                    int highlightStatus = findHighlightStatus(thesisID);
                    if(highlightStatus==0){
                        aView.setHighlightStatus("Mark");
                    }
                    else{
                        aView.setHighlightStatus("Unmark");
                    }
                    aView.setThesisName(rs.getString("THESISNAME"));
                    aView.setAuthorName(rs.getString("AUTHORNAME"));
                    aView.setCourseNo(rs.getString("COURSENO"));

                    keywords[keywordCounter] = rs.getString("KEYWORD");

                    aView.setKeywords(keywords);
                    Clob clob = rs.getClob("ABSTRACT");
                    if (clob != null) {  
                        aView.setAbstractCLOB(clob.getSubString(1, (int) clob.length()));
                    }
                    aView.setAttachmentLink(rs.getString("ATTACHMENTLINK"));
                    aView.setScreencastLink(rs.getString("SCREENCASTLINK"));
                    aView.setLiveLink(rs.getString("LIVELINK"));
                    aView.setUploadDate(df.format(rs.getDate("UPLOADDATE"))); 
                    int nextThesisID = 0;
                    
                    /* advance the cursor by one to peek at the next row */
                    int rowSaver = rs.getRow();
                    while(rs.next() && nextCounter == 0){
                        nextThesisID = Integer.parseInt(rs.getString("THESISID"));
                        if (thesisID == nextThesisID){
                            keywordCounter++;
                            keywords[keywordCounter] = rs.getString("KEYWORD");
                        }else{
                            rs.absolute(rowSaver);
                            nextCounter = 1;
                        } 
                    }                            
                viewCollection.add(aView);
            }
            
            
            rs.close();
            stmt.close();
            DBConn.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
                   
        return viewCollection;
    }

    public ViewBean detailsRequest(int thesisID){
        String baseSelect = "SELECT"
                + " IT353.ACCOUNT.FIRSTNAME || ' ' || IT353.ACCOUNT.LASTNAME AS AUTHORNAME,"
                + " IT353.THESIS.THESISID, "
                + " IT353.THESIS.THESISNAME, "
                + " IT353.THESIS.COURSENO, "
                + " IT353.THESIS.ABSTRACT, "
                + " IT353.THESIS.ATTACHMENTLINK, "
                + " IT353.THESIS.SCREENCASTLINK, "
                + " IT353.THESIS.LIVELINK, "
                + " IT353.THESIS.UPLOADDATE, "
                + " IT353.KEYWORD.KEYWORD ";
        String fromClause = "FROM IT353.ACCOUNT ";
        String joinClause = "JOIN IT353.THESIS ON IT353.THESIS.ACCOUNTID = IT353.ACCOUNT.ACCOUNTID " +
            "JOIN IT353.KEYASSIGN ON IT353.KEYASSIGN.THESISID = IT353.THESIS.THESISID " +
            "JOIN IT353.KEYWORD ON IT353.KEYWORD.KEYWORDID = IT353.KEYASSIGN.KEYWORDID ";
        String whereClause = "WHERE IT353.THESIS.THESISID = " + thesisID;
        String orderByClause = "ORDER BY IT353.THESIS.THESISID ASC";
        String query = baseSelect + fromClause + joinClause + whereClause + orderByClause;
        ViewBean aView = performDetailSearch(query);
       
        return aView;
    }
    
    public ArrayList findSimilar(ViewBean aView){
        String baseSelect = "SELECT"
                + " IT353.ACCOUNT.FIRSTNAME || ' ' || IT353.ACCOUNT.LASTNAME AS AUTHORNAME,"
                + " IT353.THESIS.THESISID, "
                + " IT353.THESIS.THESISNAME, "
                + " IT353.THESIS.COURSENO, "
                + " IT353.THESIS.ABSTRACT, "
                + " IT353.THESIS.ATTACHMENTLINK, "
                + " IT353.THESIS.SCREENCASTLINK, "
                + " IT353.THESIS.LIVELINK, "
                + " IT353.THESIS.UPLOADDATE, "
                + " IT353.KEYWORD.KEYWORD ";
        String fromClause = "FROM IT353.ACCOUNT ";
        String joinClause = "JOIN IT353.THESIS ON IT353.THESIS.ACCOUNTID = IT353.ACCOUNT.ACCOUNTID " +
            "JOIN IT353.KEYASSIGN ON IT353.KEYASSIGN.THESISID = IT353.THESIS.THESISID " +
            "JOIN IT353.KEYWORD ON IT353.KEYWORD.KEYWORDID = IT353.KEYASSIGN.KEYWORDID ";
           
        String keywords = "";
        if(aView.getKeywords().length != 0){
            for(int i=0; i < aView.getKeywords().length; i++){
                if (i == aView.getKeywords().length-1){
                    /* If it's the last one, no comma at end. */
                    keywords = keywords + "'" + aView.getKeywords()[i] + "'";
                }else{
                    keywords = keywords + "'" + aView.getKeywords()[i] + "',";
                }        
            }
        }

        String whereClause = "WHERE IT353.KEYWORD.KEYWORD IN (" + keywords + ")";
        String orderByClause = "ORDER BY IT353.THESIS.THESISID ASC";
        String query = baseSelect + fromClause + joinClause + whereClause + orderByClause;
        ArrayList searchResults = performSearch(query);
        return searchResults;
    }
    
    public ViewBean performDetailSearch(String query){
        ViewBean aView = new ViewBean();
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
        
        try {         
            String myDB = "jdbc:derby://localhost:1527/IT353";  
            Connection DBConn = DriverManager.getConnection(myDB, "itkstu", "student");
            Statement stmt = DBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(query);
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
            String[] keywords = new String[20];
            int nextCounter = 0;
            Boolean EOF = false;
            int keywordCounter = 0;
            
            int thesisID = 0;

            
            while (rs.next()) {
                
                    keywordCounter = 0;
                    nextCounter = 0;
                    
                    thesisID = Integer.parseInt(rs.getString("THESISID"));
                    aView.setThesisID(Integer.parseInt(rs.getString("THESISID")));
                    int highlightStatus = findHighlightStatus(thesisID);
                    if(highlightStatus==0){
                        aView.setHighlightStatus("Mark");
                    }
                    else{
                        aView.setHighlightStatus("Unmark");
                    }
                    aView.setThesisName(rs.getString("THESISNAME"));
                    aView.setAuthorName(rs.getString("AUTHORNAME"));
                    aView.setCourseNo(rs.getString("COURSENO"));

                    keywords[keywordCounter] = rs.getString("KEYWORD");

                    
                    Clob clob = rs.getClob("ABSTRACT");
                    if (clob != null) {  
                        aView.setAbstractCLOB(clob.getSubString(1, (int) clob.length()));
                    }
                    aView.setAttachmentLink(rs.getString("ATTACHMENTLINK"));
                    aView.setScreencastLink(rs.getString("SCREENCASTLINK"));
                    aView.setLiveLink(rs.getString("LIVELINK"));
                    aView.setUploadDate(df.format(rs.getDate("UPLOADDATE"))); 
                    
                    /* advance the cursor by one to peek at the next row */
                    int nextThesisID;
                    int rowSaver = rs.getRow();
                    while(rs.next() && nextCounter == 0){
                        nextThesisID = Integer.parseInt(rs.getString("THESISID"));
                        if (thesisID == nextThesisID){
                            keywordCounter++;
                            keywords[keywordCounter] = rs.getString("KEYWORD");
                        }else{
                            rs.absolute(rowSaver);
                            nextCounter = 1;
                        } 
                    }      
            }
            keywordCounter++;
            String[] keywordsFinal = new String[keywordCounter];
            for (int i = 0; i < keywordCounter; i++){
                keywordsFinal[i] = keywords[i];
            }
            aView.setKeywords(keywordsFinal);
            
            rs.close();
            stmt.close();
            DBConn.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        
        return aView;
    }
    
    public void incrementViewCount(int thesisID){
        String sqlSelect = "SELECT IT353.THESIS.NOTIMESVIEWED FROM IT353.THESIS WHERE IT353.THESIS.THESISID = " + thesisID;
        int noTimesViewed = getNoTimesView(sqlSelect);
        noTimesViewed++;
        String sqlUpdate = "UPDATE IT353.THESIS SET NOTIMESVIEWED = " + noTimesViewed + " WHERE IT353.THESIS.THESISID = " + thesisID;
        setNoTimesView(sqlUpdate);
    }
    
    private int getNoTimesView(String query){
        int noTimesView = 0;
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
        
        try {         
            String myDB = "jdbc:derby://localhost:1527/IT353";  
            Connection DBConn = DriverManager.getConnection(myDB, "itkstu", "student");
            Statement stmt = DBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(query);
            rs.next();
            noTimesView = rs.getInt("NOTIMESVIEWED");
            rs.close();
            stmt.close();
            DBConn.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        
        return noTimesView;
    }
    
    private int setNoTimesView(String query){
        int rowCount = 0;
        
        try {
            DBHelper.loadDriver("org.apache.derby.jdbc.ClientDriver");
            // if doing the above in Oracle: DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            String myDB = "jdbc:derby://localhost:1527/IT353";
            // if doing the above in Oracle:  String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            Connection DBConn = DBHelper.connect2DB(myDB, "itkstu", "student");
            // With the connection made, create a statement to talk to the DB server.
            // Create a SQL statement to query, retrieve the rows one by one (by going to the
            // columns), and formulate the result string to send back to the client.
            Statement stmt = DBConn.createStatement();

            //first get the thesisid for the submission being updated
            
            rowCount = stmt.executeUpdate(query);
             } catch (SQLException e) {
            System.err.println("ERROR: Problems with SQL insert/select/update in UpdateThesis()");
            System.err.println(e.getMessage());
        }
        
        return rowCount;
    }
    
    public void incrementDownCount(int thesisID){
        String sqlSelect = "SELECT IT353.THESIS.NOTIMESDOWN FROM IT353.THESIS WHERE IT353.THESIS.THESISID = " + thesisID;
        int noTimesDown = getNoTimesDown(sqlSelect);
        noTimesDown++;
        String sqlUpdate = "UPDATE IT353.THESIS SET NOTIMESDOWN = " + noTimesDown + " WHERE IT353.THESIS.THESISID = " + thesisID;
        setNoTimesDown(sqlUpdate);
    }
    
    private int getNoTimesDown(String query){
        int noTimesDown = 0;
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
        
        try {         
            String myDB = "jdbc:derby://localhost:1527/IT353";  
            Connection DBConn = DriverManager.getConnection(myDB, "itkstu", "student");
            Statement stmt = DBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(query);
            while(rs.next()){
            noTimesDown = Integer.parseInt(rs.getString("NOTIMESDOWN"));
            }
            rs.close();
            stmt.close();
            DBConn.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
      
        return noTimesDown;
    }
    
    private int setNoTimesDown(String query){
        int rowCount = 0;
        
        try {
            DBHelper.loadDriver("org.apache.derby.jdbc.ClientDriver");
            // if doing the above in Oracle: DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            String myDB = "jdbc:derby://localhost:1527/IT353";
            // if doing the above in Oracle:  String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            Connection DBConn = DBHelper.connect2DB(myDB, "itkstu", "student");
            // With the connection made, create a statement to talk to the DB server.
            // Create a SQL statement to query, retrieve the rows one by one (by going to the
            // columns), and formulate the result string to send back to the client.
            Statement stmt = DBConn.createStatement();

            //first get the thesisid for the submission being updated
            
            rowCount = stmt.executeUpdate(query);
             } catch (SQLException e) {
            System.err.println("ERROR: Problems with SQL insert/select/update in UpdateThesis()");
            System.err.println(e.getMessage());
        }
        
        return rowCount;
    }
    
    public ArrayList getCurrentSubscriptions(String userID){
        String sqlSelect = "SELECT IT353.KEYWORD.KEYWORD FROM IT353.KEYWORD WHERE"
                + "IT353.ACCOUNT.ULID = '" + userID;
        return getUserSubscriptions(sqlSelect);
    }
    
    public void addSubscription(String userID, String keyword){
        String insertStatement = "INSERT INTO IT353.SUBSCRIPTION VALUES";
    }
    
    public void removeSubscription(String userID, String keyword){
        String deleteStatement = "DELETE FROM IT353.SUBSCRIPTION WHERE IT353.SUBSCRIPTION.ACCOUNTID = '" + userID + "' AND IT353.KEYWORD.KEYWORD = '" + keyword + "'";
    }    
    
    public ArrayList getAvailableSubscriptions(String userID){
        ArrayList availableSubscriptions = null;
        
        return availableSubscriptions;
    }    
    
    public String[] getSubscriberEmails(String keyword){
        int keywordID = findKeywordIDByKeyword(keyword);
        String sqlSelect = "SELECT IT353.ACCOUNT.EMAIL FROM IT353.ACCOUNT "
                + "JOIN IT353.SUBSCRIPTION ON IT353.SUBSCRIPTION.ACCOUNTID = IT353.ACCOUNT.ACCOUNTID "
                + "WHERE IT353.SUBSCRIPTION.KEYWORDID = " + keywordID;
        return emailSearch(sqlSelect);
    }
    
    private String[] emailSearch(String query){
        String[] emails = null;
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
        
        try {         
            String myDB = "jdbc:derby://localhost:1527/IT353";  
            Connection DBConn = DriverManager.getConnection(myDB, "itkstu", "student");
            Statement stmt = DBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(query);
            
            int counter = 0;            
            
            rs.last();
            int noOfEmails = rs.getRow();
            rs.beforeFirst();
            
            emails = new String[noOfEmails];
            
            while(rs.next()){
                emails[counter] = rs.getString("EMAIL");
                counter++;
            }
            
            rs.close();
            stmt.close();
            DBConn.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        
        return emails;
    }
    
    private ArrayList getUserSubscriptions(String query){
        ArrayList keywords = null;
        
        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
        
        try {         
            String myDB = "jdbc:derby://localhost:1527/IT353";  
            Connection DBConn = DriverManager.getConnection(myDB, "itkstu", "student");
            Statement stmt = DBConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt.executeQuery(query);
                      
            while(rs.next()){
                String keyword = new String();
                keyword = rs.getString("KEYWORD");
                keywords.add(keyword);
            }
            
            rs.close();
            stmt.close();
            DBConn.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
        return keywords;
    }
    
    private int findKeywordIDByKeyword(String keyword){
        String query = "SELECT IT353.KEYWORD.KEYWORDID FROM IT353.KEYWORD WHERE IT353.KEYWORD.KEYWORD = '" + keyword + "'";
        int keywordID = 0;
        
        Connection DBConn = null;
        try {
            DBHelper.loadDriver("org.apache.derby.jdbc.ClientDriver");
            String myDB = "jdbc:derby://localhost:1527/IT353";
            DBConn = DBHelper.connect2DB(myDB, "itkstu", "student");

            Statement stmt = DBConn.createStatement();
            
            ResultSet rs = stmt.executeQuery(query);
            
            while(rs.next())
                keywordID = rs.getInt("KEYWORDID");
            
            rs.close();
            stmt.close();
            } catch (Exception e) {
                System.err.println("ERROR: Problems with SQL select");
                e.printStackTrace();
            }
            try {
                DBConn.close();
            } catch (SQLException e) {
                System.err.println(e.getMessage());
            }
        return keywordID;
        
    }
    
    public int findHighlightStatus(int thesisId){
        String query = "SELECT COUNT(*) AS THESISCOUNT FROM IT353.HIGHLIGHT WHERE THESISID = "+ thesisId;
        int thesisCount=0;
        try {
            DBHelper.loadDriver("org.apache.derby.jdbc.ClientDriver");

            // if doing the above in Oracle: DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            String myDB = "jdbc:derby://localhost:1527/IT353";
            // if doing the above in Oracle:  String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            Connection DBConn = DBHelper.connect2DB(myDB, "itkstu", "student");

            // With the connection made, create a statement to talk to the DB server.
            // Create a SQL statement to query, retrieve the rows one by one (by going to the
            // columns), and formulate the result string to send back to the client.
            Statement stmt = DBConn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                thesisCount = Integer.parseInt(rs.getString("THESISCOUNT"));
            }           
            DBConn.close();
        } catch (SQLException e) {
            System.err.println("ERROR: Problems with SQL insert in findHighlightStatus()");
            System.err.println(e.getMessage());
        }
        return thesisCount;
    }
    
    @Override
    public int performMark(int thesisID){
        int rowCount = 0;
        int nextHighlightId = findNextSequenceValue("highlight_id_seq");
        String markQuery = "INSERT INTO IT353.HIGHlIGHT VALUES (" + nextHighlightId + ", " + thesisID + ")";
        try {
            DBHelper.loadDriver("org.apache.derby.jdbc.ClientDriver");
//            DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            // if doing the above in Oracle: DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            String myDB = "jdbc:derby://localhost:1527/IT353";
//            String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            // if doing the above in Oracle:  String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            Connection DBConn = DBHelper.connect2DB(myDB, "itkstu", "student");
            // With the connection made, create a statement to talk to the DB server.
            // Create a SQL statement to query, retrieve the rows one by one (by going to the
            // columns), and formulate the result string to send back to the client.
            Statement stmt = DBConn.createStatement();
            rowCount = stmt.executeUpdate(markQuery);
            System.out.println("Marking rows updated : " + rowCount);
            DBConn.close();
        } catch (SQLException e) {
            System.err.println("ERROR: Problems with SQL select in performMark()");
            System.err.println(e.getMessage());
        }
        return rowCount;
    }
    
    @Override
    public int performUnmark(int thesisID){
        int rowCount = 0;

        String markQuery = "DELETE FROM IT353.HIGHlIGHT WHERE THESISID = " + thesisID;
        try {
            DBHelper.loadDriver("org.apache.derby.jdbc.ClientDriver");
            //            DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            // if doing the above in Oracle: DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            String myDB = "jdbc:derby://localhost:1527/IT353";
            //            String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            // if doing the above in Oracle:  String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            Connection DBConn = DBHelper.connect2DB(myDB, "itkstu", "student");
            // With the connection made, create a statement to talk to the DB server.
            // Create a SQL statement to query, retrieve the rows one by one (by going to the
            // columns), and formulate the result string to send back to the client.
            Statement stmt = DBConn.createStatement();
            rowCount = stmt.executeUpdate(markQuery);
            System.out.println("Marking rows deleted : " + rowCount);
            DBConn.close();
        } catch (SQLException e) {
            System.err.println("ERROR: Problems with SQL select in performUnmark()");
            System.err.println(e.getMessage());
        }
        return rowCount;
    }
    
    public int findNextSequenceValue(String sequenceName) {
        sequenceName = sequenceName.trim().toUpperCase();
        System.out.println("sequence name = " + sequenceName);
        int nextValue = 0;
        String retrieveIDQuery = "VALUES NEXT VALUE FOR " + sequenceName;
        try {
            DBHelper.loadDriver("org.apache.derby.jdbc.ClientDriver");
//            DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            // if doing the above in Oracle: DBHelper.loadDriver("oracle.jdbc.driver.OracleDriver");
            String myDB = "jdbc:derby://localhost:1527/IT353";
//            String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            // if doing the above in Oracle:  String myDB = "jdbc:oracle:thin:@oracle.itk.ilstu.edu:1521:ora478";
            Connection DBConn = DBHelper.connect2DB(myDB, "itkstu", "student");

            // With the connection made, create a statement to talk to the DB server.
            // Create a SQL statement to query, retrieve the rows one by one (by going to the
            // columns), and formulate the result string to send back to the client.
            Statement stmt = DBConn.createStatement();

            ResultSet rs = stmt.executeQuery(retrieveIDQuery);
            while (rs.next()) {
                nextValue = rs.getInt(1);
            }
            DBConn.close();
        } catch (SQLException e) {
            System.err.println("ERROR: Problems with SQL select in findNextSequenceValue()");
            System.err.println(e.getMessage());
        }
        System.out.println("next value = " + nextValue);
        return nextValue;
    }
            @Override
    public ArrayList highlightSearch(){
        String query = "SELECT IT353.ACCOUNT.FIRSTNAME || ' ' || IT353.ACCOUNT.LASTNAME AS AUTHORNAME, "
                + "IT353.THESIS.THESISID, "
                + "IT353.THESIS.THESISNAME, "
                + "IT353.THESIS.COURSENO,  "
                + "IT353.THESIS.ABSTRACT,  "
                + "IT353.THESIS.ATTACHMENTLINK, " 
                + "IT353.THESIS.SCREENCASTLINK, "
                + "IT353.THESIS.LIVELINK, "
                + "IT353.THESIS.UPLOADDATE, "
                + "IT353.KEYWORD.KEYWORD " 
                + "FROM IT353.ACCOUNT JOIN IT353.THESIS ON IT353.THESIS.ACCOUNTID = IT353.ACCOUNT.ACCOUNTID " 
                + "JOIN IT353.KEYASSIGN ON IT353.KEYASSIGN.THESISID = IT353.THESIS.THESISID " 
                + "JOIN IT353.KEYWORD ON IT353.KEYWORD.KEYWORDID = IT353.KEYASSIGN.KEYWORDID " 
                + "WHERE IT353.THESIS.THESISID IN (SELECT IT353.HIGHLIGHT.THESISID FROM IT353.HIGHLIGHT)";
        ArrayList searchResults = performSearch(query);
        /* Return the search results */
        return searchResults;
    }
}
