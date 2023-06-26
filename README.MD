# Nicknames
Proxy-wide synchronized nicknames via SQL database with PlaceholderAPI support.

## Initial Ask

I want a Paper compatible plugin that allows users with permission to set another users' "nickname". The nicknames should be stored in a MySQL database. The nicknames should be exposed as PlaceholderAPI placeholders.

I will install this plugin on each server and configure our chat plugin to use the placeholder as the user's name in chat.

Commands and Permissions:
- /nickname <user> - no permission node - get a user's nickname
- /nickname clear <user> - nicknames.clear - set a user's nickname back to their IGN
- /nickname set <user> <nickname> - nicknames.set - set a user's nickname to the value provided

AC:
- If a user has no nickname, the value of the placeholder should be their IGN
- Nicknames persist across IGN changes
- Staff have permission to set someone's nickname, users do not
- nickname is available as placeholderAPI placeholder
- nickname is the same on every server

TC:

- New players need a placeholder value for their name in chat to work. On player join, check if user has a DB entry, and add one if they do not.
- To set nickname for player on another server, we cannot validate the player is online or it will fail. Instead, check if that exists in the DB and provide an error that the user is invalid if they do not.
- Must leverage UUIDs to keep users' nicknames after they change their IGN
- After an IGN change, users' nickname will need to be updated to their new IGN if they do not have a custom nickname
- Must leverage MySQL DB to sync across servers
