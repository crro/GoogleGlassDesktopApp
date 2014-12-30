# Java Desktop Application for Google Glass Lecturing App

This Java Desktop App runs on the user’s computer. This application has two main purposes:
- Keep the server updated on the current state of the presentation
- Receive and execute the commands sent from the Glass device through the server.
These two objectives are accomplished by executing AppleScripts to manipulate and obtain information from the ongoing
Microsoft Powerpoint presentation.

This application establishes a permanent connection to the server through a WebSocket that allows the server to
continuously forward commands sent from the Glass device.

Additionally, since the server times our if no command is received through the connection, this
application constantly pings the server every 50 seconds.

Furthermore, every second, the application cheeks on the PowerPoint presentation and sends
the most updated slide to the server. This allow the presentation and the glass device to be in
full synchronization.

## Main.java

This is the file where everything begins. This file contains the address of the scripts and is in charge of executing the AppleScript commands.
- executePresentationTask(…) is a generic method used to execute any AppleScript
- getCurrentIndex(…) retrieves the current index of the ongoing presentation
- getAllNotes(…) obtains all the notes

## PowerPointSocket.java

This is the class that models the WebSocket that establishes the permanent connection with the
server. Depending on the message received, it executes a particular AppleScript to perform a
task.

This class also creates two timers:
- One in charge of keeping the connection open by constantly pinging the server.
- The other one in charge of constantly requesting the current slide of the presentation

## SessionHQ.java

This class stores the different WebSocket sessions of the application. At the moment, there is only one session.

## Scripts Folder

This folder contains all the scripts used for the interactions with PowerPoint. They are both in .txt and in .scpt
format.

## ImageUploader.java

This class has no usefulness to the application as a whole but it was a great example of how to
get an image to the server and back. I used it to implement the display of images on the glass
device. I kept it for future reference.

