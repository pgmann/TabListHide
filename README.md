##TabListHide 

Quick links · [Download Latest Build](https://ci.pgmann.cf/job/TabListHide/lastSuccessfulBuild/artifact/target/TabListHide-1.1-SNAPSHOT.jar) · [Jenkins](https://ci.pgmann.cf/job/TabListHide/) · [JavaDoc](https://ci.pgmann.cf/job/TabListHide/javadoc) · [Maven Repo](https://mvn.pgmann.cf/) · [License](https://www.gnu.org/licenses/gpl-3.0.en.html)

### ~ How it works ~

TabListHide is a plugin for Bukkit. As the name suggests, it allows players to be hidden from the tab list.
Once players are hidden, they will remain hidden until either the server restarts or the `/tlh show` command is executed.

####Permissions:
- `tablisthide.hide`: Automatically hides the user from the tab list when they join
- `tablisthide.admin`: Gives the user permission to use this plugin's commands

####Commands:
- `/tlh help`: Shows a list of all commands shown here
- `/tlh hide [player] [silent]`: Hides the player (or the sender) from the tab list
- `/tlh show [player] [silent]`: Shows the player (or the sender) in the tab list again

### ~ License ~

This project is licensed under the [GNU General Public License v3.0 (GPLv3)](https://www.gnu.org/licenses/gpl-3.0.en.html). A summary of this licence is [available here](https://www.tldrlegal.com/l/gpl-3.0).

### ~ Obtaining a Build ~

The source code of this repository is automatically built and [is always available here](https://ci.pgmann.cf/job/TabListHide/).
In order to build this project yourself, it is recommended to use **Maven**.
If you don't, you will need to import the various dependencies manually to be able to build the project.
