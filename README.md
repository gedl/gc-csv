# gc-csv
Command line utility to export Google Contacts  to a Comma Separated File

I wrote this tool because couldn't find any command line option to export google contacts to a CSV file.

A very simple command line tool to generate CSV files for google contacts.

Requires users to create an application in the google dev console and configure OAuth client id and secret. Check here: https://developers.google.com/identity/protocols/OAuth2

This can be compiled from source (it's a maven project) but ready-to-use jar files are available from the releases page, here: https://github.com/gedl/gc-csv/releases


usage:   [-h] -C <id> | -c <path>  -s <secret> | -S <path>  [--page-size <size>] [--max-results
       <max>] [--group-id <id>] [--email <email>] [-f <path>] [-p] [--deaccent] [--output-header
       <header>] [--output-mapping <mapping>] -d <path> [-v]
 -h,--help                        Prints this message and exits
 -C,--client-id <id>              The OAuth v2 client id
 -c,--client-id-file <path>       The file containing the OAuth v2 client id
 -s,--client-secret <secret>      The OAuth v2 client secret
 -S,--client-secret-file <path>   The file containing the OAuth v2 client secret
    --page-size <size>            The page size for the Google Contacts request (default is 50)
    --max-results <max>           The maximum number of contacts to fetch from Google Contacts
                                  (default is 2147483647)
    --group-id <id>               The group id to filter the contacts with (requires email)
    --email <email>               The email to filter the contacts with (requires group-id)
 -f,--output-file <path>          The file to write the CSV output to. If not set, no file will be
                                  written.
 -p,--print                       Whether to print the output
    --deaccent                    Replace diacritics into plain ASCII counterparts
    --output-header <header>      The header of the CSV file. Arity has to match the mapping. Do not
                                  use commas, enclose names with spaces in quotes. Default is Name,
                                  Number 1, Number 2, Email
    --output-mapping <mapping>    The ordered google contacts field name to populate the columns.
                                  Arity has to match number of fields specified in the header.
                                  Repeat google contact fields to iterate lists. Default is
                                  NAME=>Name, PHONE_NUMBER=>Number 1, PHONE_NUMBER=>Number 2,
                                  EMAIL=>Email. Valid fields are: NAME, PHONE_NUMBER, EMAIL, NONE
                                  (case sensitive).
 -d,--storage-dir <path>          The directory to keep the OAuth V2refresh token in. It is
                                  recommended to use a full path with leading and trailing slash
 -v,--verbose                     Verbose mode. Includes java stack traces if an exception occurs
 
 For a quick test run use with  are -C -S -d. Those are the client id, client secret and the data store, where the tool will store the refresh token (avoiding asking the users to authorize the app to access their contacts on each run).
 You should also specify -p or -f <path> in order to see some results.
