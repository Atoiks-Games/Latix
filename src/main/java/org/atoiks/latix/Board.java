package org.atoiks.latix;

public final class Board implements java.io.Serializable {
    private enum TileState {
	EMPTY, P1_PIECE, P2_PIECE, RESERVED
    }

    private static final long serialVersionUID = 1279125383L;
    public static final int DIMENSION = 9;
    
    private final TileState[][] tiles;
    
    public Board() {
	tiles = new TileState[DIMENSION][DIMENSION];
    }

    public void reset() {
	for (int i = 0; i < DIMENSION; ++i) {
	    for (int j = 0; j < DIMENSION; ++j) {
		// null tiles indicate non-passable tiles
		if (i == 4 && j >= 3 && j <= 5) {
		    tiles[i][j] = TileState.RESERVED;
		    continue;
		}

		// P1 set up
		if (i == 0 && j >= 1 && j <= 7) {
		    tiles[i][j] = j == 4 ? TileState.RESERVED : TileState.P1_PIECE;
		    continue;
		}
		if (i == 1 && j >= 2 && j <= 6) {
		    tiles[i][j] = TileState.P1_PIECE;
		    continue;
		}
		
		// P2 set up
		if (i == 8 && j >= 1 && j <= 7) {
		    tiles[i][j] = j == 4 ? TileState.RESERVED : TileState.P2_PIECE;
		    continue;
		}
		if (i == 7 && j >= 2 && j <= 6) {
		    tiles[i][j] = TileState.P2_PIECE;
		    continue;
		}
		tiles[i][j] = TileState.EMPTY;
	    }
	}
    }
    
    public boolean trySpawnP1At(int x, int y) {
	if (x == 4 && y == 0 && tiles[y][x] != TileState.P1_PIECE) {
	    tiles[y][x] = TileState.P1_PIECE;
	    return true;
	}
	return false;
    }
    
    public boolean trySpawnP2At(int x, int y) {
	if (x == 4 && y == 8 && tiles[y][x] != TileState.P2_PIECE) {
	    tiles[y][x] = TileState.P2_PIECE;
	    return true;
	}
	return false;
    }

    public static boolean isRegularTile(int x, int y) {
	// isNonPassableTile will handle out of bound coordinates
	return !(isNonPassableTile(x, y) || isDiagonalTile(x, y));
    }

    public static boolean isNonPassableTile(int x, int y) {
	if (x < 0 || x >= DIMENSION || y < 0 || y >= DIMENSION) {
	    // out of bounds, consider it non-passable
	    return true;
	}
	
        return (y == 4 && (x == 3 || x == 4 || x == 5))
	    || isSpawnerTile(x, y);
    }

    public static boolean isSpawnerTile(int x, int y) {
	return x == 4 && (y == 0 || y == 8);
    }

    public static boolean isDiagonalTile(int x, int y) {
	return ((y == 1 || y == 7) && (x == 2 || x == 6))
	    || ((y == 2 || y == 6) && (x == 1 || x == 7))
	    || ((y == 3 || y == 5) && x == 4);
    }

    /**
     * Checks to see if there is a winner
     *
     * @returns 1 if P1 won, 2 if P2 won, 0 if draw,
     *          anything else if no one won
     */
    public int getWinner() {
	// These do not include the ones on the 'o' tiles
	int p1RemPieces = 0;
	int p2RemPieces = 0;

        boolean p1HasPossibleMoves = true;
        boolean p2HasPossibleMoves = true;
	
	for (int y = 0; y < DIMENSION; ++y) {
	    for (int x = 0; x < DIMENSION; ++x) {
		if (!isNonPassableTile(x, y)) {
		    switch (tiles[y][x]) {
		    case P1_PIECE:
			++p1RemPieces;
			if (p1HasPossibleMoves) {
			    final int[] tiles = getMovementOptions(x, y, 1);
			    for (int i = tiles.length; i > 0; --i) {
				if (tiles[i - 1] < 0) {
				    p1HasPossibleMoves = false;
				    break;
				}
			    }
			}
			break;
		    case P2_PIECE:
			++p2RemPieces;
			if (p2HasPossibleMoves) {
			    final int[] tiles = getMovementOptions(x, y, 2);
			    for (int i = tiles.length; i > 0; --i) {
				if (tiles[i - 1] < 0) {
				    p2HasPossibleMoves = false;
				    break;
				}
			    }
			}
			break;
		    }
		}
	    }
	}

	if (p1RemPieces == 0) {
	    // check if p2 also has no more pieces (draw)
	    return p2RemPieces == 0 ? 0 : 2;
	}
	if (p2RemPieces == 0) {
	    return 1;
	}

	if (!p1HasPossibleMoves) {
	    // check if p2 also has no more possible moves (draw)
	    return p2HasPossibleMoves ? 2 : 0;
	}
	if (!p2HasPossibleMoves) {
	    return 1;
	}

	// anything but 0, 1, 2 will work
	return -1;
    }

    public boolean hasP1Piece(int x, int y) {
	return tiles[y][x] == TileState.P1_PIECE;
    }

    public boolean hasP2Piece(int x, int y) {
	return tiles[y][x] == TileState.P2_PIECE;
    }

    private boolean isBlocked(int x, int y, int playerView) {
	// isNonPassableTile handles out of bound coordinates
	if (isNonPassableTile(x, y)) {
	    return true;
	}
        switch (playerView) {
	case 1:
	    return tiles[y][x] == TileState.P1_PIECE;
	case 2:
	    return tiles[y][x] == TileState.P2_PIECE;
	default:
	    return tiles[y][x] != TileState.EMPTY;
	}
    }

    public int[] getMovementOptions(int x, int y, int playerView) {
	// The result is in format of [x1, y1, x2, y2, ..., xn, yn]
	// if movement is impossible (or the tile could not contain
	// a piece) an empty array is returned
	//
	// It may return negative coordinates. These coordinates
	// are considered invalid.

	int[] aug = new int[0];
	if (isSpawnerTile(x, y) || isRegularTile(x, y)) {
	    // acts like a regular tile
	    // up, down    -> 1 or 2 tiles
	    // left, right -> 1 tiles
	    
	    aug = new int[] {
		x - 1, y,	// left
		x + 1, y,	// right
		x, y + 1, x, y + 2, // up * 2
		x, y - 1, x, y - 2  // down * 2
	    };
	} else if (isDiagonalTile(x, y)) {
	    switch (y) {
	    case 1:
		aug = new int[] {
		    x - 1, 0, x + 1, 0,
		    x - 1, 2, x + 1, 2,
		    x - 2, 3, x + 2, 3
		};
		break;
	    case 7:
		aug = new int[] {
		    x - 2, 5, x + 2, 5,
		    x - 1, 6, x + 1, 6,
		    x - 1, 8, x + 1, 8
		};
		break;
	    case 2:
		aug = new int[] {
		    x - 1, 1, x + 1, 1,
		    x - 1, 3, x + 1, 3,
		    x - 2, 0, x + 2, 0
		};
		break;
	    case 6:
		aug = new int[] {
		    x - 1, 7, x + 1, 7,
		    x - 1, 5, x + 1, 5,
		    x - 2, 8, x + 2, 8
		};
		break;
	    case 3:
		aug = new int[] {
		    x - 1, 2, x + 1, 2,
		    x - 2, 1, x + 2, 1,
		    x - 3, 0, x + 3, 0
		};
		break;
	    case 5:
		aug = new int[] {
		    x - 1, 6, x + 1, 6,
		    x - 2, 7, x + 2, 7,
		    x - 3, 8, x + 3, 8
		};
		break;
	    }
	    
	    if (aug.length == 0) {
		throw new AssertionError("Case of " + x + "," + y + " not handled!");
	    }
	}

	for (int i = 0; i < aug.length; i += 2) {
	    if (isBlocked(aug[i], aug[i + 1], playerView)) {
		aug[i] = aug[i + 1] = -1;
	    }
	}
	return aug;
    }

    public void bruteMove(int x1, int y1, int x2, int y2) {
	// Paths are not validated in this method
	// just make sure it is a piece being moved
	// (no RESERVED / EMPTY moving) and that
	// its not moving to itself
	if (x1 == x2 && y1 == y2) return;
	switch (tiles[y1][x1]) {
	case P1_PIECE:
	case P2_PIECE:
	    tiles[y2][x2] = tiles[y1][x1];
	    tiles[y1][x1] = isSpawnerTile(x1, y1) ? TileState.RESERVED : TileState.EMPTY;
	    break;
	}
    }

    public void print(final java.io.PrintStream out) {
	for (int i = 0; i < DIMENSION; ++i) {
	    for (int j = 0; j < DIMENSION; ++j) {
		switch (tiles[i][j]) {
		case P1_PIECE:
		    out.print('1');
		    break;
		case P2_PIECE:
		    out.print('2');
		    break;
		case EMPTY:
		    out.print('+');
		    break;
		case RESERVED:
		    out.print('x');
		    break;
		}
	    }
	    out.println("");
	}
    }
}
