For the ONLYOFFICE Applications example to work properly you need to do the following:

1. Download and extract the Example.zip into a directory selected for it, for instance, C:\OnlyofficeExample

2. Run the IIS Manager (C:\Windows\system32\inetsrv\InetMgr.exe or Start -> All Programs -> Administrative Tools -> Internet Information Services (IIS) Manager for Windows Server 2003)

3. Add the unpacked Example to the list of sites with the physical path to the C:\OnlyofficeExample (or the one you selected) folder.

4. Make sure that the user account under which the IIS NetworkService process is executed has the write access rights for the C:\OnlyofficeExample\App_Data folder (this is significant for the IIS later than 6.0).

5. Make the created ONLYOFFIC™ Applications example available through IP or domain name so that it could be connected from the web by the document service for proper document rendering.





