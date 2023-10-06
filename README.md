# README #

VoltSeConvert is used to do a crude, once-off conversion from Oracle or TimesTen to VoltDB

## What is this repository for? ##

This rep contains java sources and JDBC drivers needed to compile the converter. https://bitbucket.org/voltdbseteam/voltseconvertoracleexample contains example code.

## Dependencies ##

* VoltSeUtil

## What do I need to use it? ##

* Java 1.8
* Oracle 10.2 or greater, or TimesTen, running the target application.

## Usage ##

### Running Converter ###

The main class is Converter.java. 

The generic command line is:

ORACLE host port sid user pass otheruser package_name dir sqlpasses


### Parameters ###

* **DB type** (ORACLE or TIMESTEN)
* **host** 
* **sid** (the oracle or timesten instance id)
* **user** - login user
* **pass** - login pass
* **otheruser** - user who owns procedures. A common pattern in Oracle/Timesten is for different users to own code versus run code.
* **package_name** - desired java package name
* **dir** - output directory. child directories sql and src will be created.
* **sqlpasses** - how many times the convertor looks for new parsed SQL statements, at one minute intervals.


An example set of parameters would be:

ORACLE 
orcl2.cpolcsopv6zq.us-east-1.rds.amazonaws.com
1521 
ORCL2 
orindademo
orindademo
otherusername 
org.foo 
/Users/drolfe/Desktop/EclipseWorkspace/voltseconvertoracleexample
1

## What it does ##

* Creates VoltDB-friendly table definitions in the sql directory. Note that we ignore FK's.
* Creates VoltDB compatible procedures that map to the target DB's procedures.

## How it does it ##

### 1. Query ALL_ARGUMENTS to get procedure names ###

Both Oracle and TimesTen provide  a data dictionary view called [ALL_ARGUMENTS](https://docs.oracle.com/cd/B28359_01/server.111/b28320/statviews_1014.htm#REFRN20015) that shows the input, input output and output parameters for each procedures. The view is semi-hierarchical - if a record or array is used as a parameter the level below will contains the fields of the record. In our first pass we simply get a list of procedure names. 

### 2. Get top level parameters for each procedure ###

For each procedure we get the top level parameters and their data types. We use this to define the inputs and outputs of our VoltDB procedures. All records, arrays and non-scalar types are mapped to VoltTable. 

### 3. Oracle only: find SQL statements ###

In the case of Oracle we query [V$SQL](https://docs.oracle.com/database/121/REFRN/GUID-2B9340D7-4AA8-4894-94C0-D5990F67BE75.htm#REFRN30246), which contains the raw text of each currently parsed SQL statement. Note that we can only find SQL statements that have been recently parsed - i.e. for procedures that someone attempted to invoke.

### 4. Get source code for procedure ###

We also query ALL_SOURCE, which contains - as the name suggests - the source code of the procedures.

### 5. Oracle Only: Match SQL statements to source code.

We then turn the SQL statements into regular expressions and search for them in our collection of procedure source code. When we find a match we create a SQLStmt object in the generated VoltDB procedure

### 6. Generate stored procedures

Each procedure will have the following:

* Input params
* Output params - defined as variables with matching code to create a VoltTable[] to to return
* SQL Statements (where available)
* Source code (commented out)

### 7. Generate Tables ###

We also create VoltDB friendly tables for the Tables and Views we find in the source schema.

## Sample Output ##

See [here](https://bitbucket.org/voltdbseteam/voltseconvertoracleexample).

## Known Issues ##

* Procedures with no parameters are ignored.