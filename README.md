# gc-csv
Command line utility to export Google Contacts  to a Comma Separated File

I wrote this tool because couldn't find any command line option to export google contacts to a CSV file.

A very simple command line tool to generate CSV files for google contacts.

Requires users to create an application in the google dev console and configure OAuth client id and secret. Check here: https://developers.google.com/identity/protocols/OAuth2

This can be compiled from source (it's a maven project) but ready-to-use jar files are available from the releases page, here: https://github.com/gedl/gc-csv/releases
 
 For a quick test run use with  are -C -S -d. Those are the client id, client secret and the data store, where the tool will store the refresh token (avoiding asking the users to authorize the app to access their contacts on each run).
 You should also specify -p or -f <path> in order to see some results.
 
 For help run with -h
