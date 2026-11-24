JACKPOT & GAMBLING MANAGER  (Fabric mod, Minecraft 1.21.11, client-side)

INSTALL
 1. Push this folder to a GitHub repo (or upload the zip contents). The workflow in .github/workflows/build.yml
    builds the jar and publishes it under Releases ("Jackpot Manager (latest build)").
 2. Put the jar in your mods folder next to Fabric API (1.21.11).

KEYS (rebindable in Controls > Jackpot Manager)
 J = open the manager     K = review pending payments

TEST BEFORE GOING LIVE  (Settings tab > Test Lab)
 - Type a sample chat line, see if it is detected, then "Feed as chat".
 - "Simulate payment" creates a [TEST] payment -> press K -> pick a game -> Win / Lose.
 - "Fill jackpot" adds test players. "Clear test data" removes everything simulated.

SAFETY
 - The mod only READS server messages already shown in your chat. It never sends money, chat or commands.
 - Win/Lose only copies text to your clipboard; you paste and press Enter yourself.
 - Detection depends on the server printing a line like "Name paid you 5.1M". If your server words it
   differently, edit the pattern in Settings (keep the groups named player and amount).

DEFAULT NUMBERS
 9 x 30.2M = 271.8M, tax 13% = 35.334M, prize 236.466M -> shown as 236.46M (amounts are truncated, never rounded up).
 Blackjack 5.1M x 1.96 = 9.996M.

SAVED DATA: config/jackpotmanager.json
