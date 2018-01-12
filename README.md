# Latix [![Build Status](https://travis-ci.org/Atoiks-Games/Latix.svg?branch=master)](https://travis-ci.org/Atoiks-Games/Latix)

A chess like game with go like concepts. Rules are listed below...

## Build Instructions

Get jdk 8 or above, then run `gradlew run`

## Toggle Switches

`r` - To restart

`s` - To save

`o` - To open a previous gamesave

`q` - To quit

## Game Board

```
  a b c d e f g h i
1 +-+-+-+-o-+-+-+-+
  | |\|/| | |\|/| |     Objects can only be placed on '+' or 'x'
2 +-+-x-+-+-+-x-+-+     '+' tiles indicate the piece can only move
  |\|/|\| | |/|\|/|        up, down, left, or right by one tile
3 +-x-+-+-+-+-+-x-+     OR up, down by two tiles
  |/|\| |\|/| |/|\|     'x' tiles indicate the piece can only move
4 +-+-+-+-x-+-+-+-+        diagonally by any number of tiles
  | | |       | | |
5 +-+-+       +-+-+
  | | |       | | |
6 +-+-+-+-x-+-+-+-+
  |\|/| |/|\| |\|/|
7 +-x-+-+-+-+-+-x-+     'o' tiles are where a new piece is added to
  |/|\|/| | |\|/|\|        existing pieces cannot move on nor
8 +-+-x-+-+-+-x-+-+        through them
  | |/|\| | |/|\| |
9 +-+-+-+-o-+-+-+-+

set up: (P2 for example)
8 +-+-A-A-A-A-A-+-+     'A' indicates the pieces
  | | | | | | | | |
9 +-A-A-A-o-A-A-A-+
```

## Winning Condition

Take all the pieces of the other player that are not located on the 'o' tile or
if the opponent resigns (AKA rage quit).

## Rules

* Similar to chess, two colors, red side always starts first.
* The rounds alternate between the two players.
* On each round, the player can choose to either get a new piece or move an existing piece.
* The movement restrictions for each piece are shown with green markers.

If the player wants to get a new piece, the player clicks on the 'o' tile on the player's side, and a new piece will appear. __You can do this a maximum of five times.__
