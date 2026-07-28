# RRT [Restock Report Tool]
Restock Report Tool for Animazed LLC written by Nolan Wright. Used to create reports based of inventory and order levels from the Shopify System. Inventory and Order Export Demo Files are not provided, and are expected to come from within the User's Shopify System.

# Staff User Guide

## Files Needed for RRT
- RRT Jar [Download from the Release Tab]
- Java Installed [Java 26ish is Fine]
  - https://www.oracle.com/java/technologies/downloads/
- Inventory Export from Shopify
  - Products > Inventory > Export
  - Make Sure All Locations are Selected
  - Select All Variants and Click Export
  - It Will Email To You after a Short Period of Time
- Orders Export from Shopify [Needed if you want Restock or Sales Reports]
  - Orders > Export > Orders By Date
  - Select the Dates Needed and Click Export Orders
  - It Will Email To You after a Short Period of Time
- Make sure all files are UnZipped [Right Click and UnZip if it's not a CSV]

## RRT Use Guide

1. Click to load the RRT. <br/>
2. It will request the Inventory Export. Find it and Click on it. <br/>
<img width="489" height="343" alt="image" src="https://github.com/user-attachments/assets/f21cbda0-9421-4cf9-b4f2-6195e7fcd6a4" /> <br/>
3. Once the file loads, you will be taken to the main screen. Here you can input your orders export and run your reports.
<img width="379" height="386" alt="image" src="https://github.com/user-attachments/assets/7b034ed6-080d-4610-a0ff-6de09216c52a" /> <br/>
4. If you are wanting to run the Restock and Sales Report, you will need to select a Location for Restock. Click the Drop Down Labeled "Please Select a Location." to designate the Restock Location. Then you may click the "Add Orders Export for All Locations" and hand over the Orders Export.
5. The data will then be loaded and ready. Click "Run Reports." to run the reports needed.
    - If you are wanting to just run the Spot Checks and Incoming Report, you can just click "Run Reports." immediately. If you want to add the Zeroes Report, select a Restock Location as described above, then click "Run Reports.".


