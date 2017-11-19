package org.atoiks.latix;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class App {

    private static final int HEIGHT = 300;
    private static final int WIDTH = 300;

    private static final byte INITING = 0b0000;
    private static final byte WAIT_FLG = 0b0001;
    private static final byte DEST_FLG = 0b1000;
    private static final byte P1_FLG = 0b0010;
    private static final byte P2_FLG = 0b0100;

    private static final byte P1_TURN0 = P1_FLG;
    private static final byte P1_TURN1 = P1_FLG | DEST_FLG;
    private static final byte P2_TURN0 = P2_FLG;
    private static final byte P2_TURN1 = P2_FLG | DEST_FLG;

    private Board board = new Board();
    private long window;

    private byte state = INITING;

    private int mouseVirtX = 0;
    private int mouseVirtY = 0;

    private int originX = 0;
    private int originY = 0;

    // see initial value in update()
    private int p1RemSpawns;
    private int p2RemSpawns;

    private int vaoIdBoard;
    private int vboIdBoard;

    private long startTime;

    public void run() {
        init();
        loop();
        destroy();

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Cannot init GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(300, 300, "Latix", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to crete GLFW window");
        }

	glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
	    if (action == GLFW_PRESS) {
		switch (key) {
		case GLFW_KEY_Q:
		    glfwSetWindowShouldClose(window, true);
		    break;
		case GLFW_KEY_R:
		    // mark state back to init (essentially a reset
		    state = INITING;
		    break;
		case GLFW_KEY_S:
		    System.err.print("saving game in latix.gamesave ");
		    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("latix.gamesave"))) {
			oos.writeObject(board);
			oos.writeInt(p1RemSpawns);
			oos.writeInt(p2RemSpawns);
			System.err.println("[DONE]");
		    } catch (java.io.IOException ex) {
			System.err.println("[FAIL]");
		    }
		    break;
		case GLFW_KEY_O:
		    System.err.print("opening game from latix.gamesave ");
		    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("latix.gamesave"))) {
			board = (Board) ois.readObject();
			p1RemSpawns = ois.readInt();
			p2RemSpawns = ois.readInt();
			System.err.println("[DONE]");
		    } catch (java.io.IOException | ClassNotFoundException ex) {
			System.err.println("[FAIL]");
		    }
		    break;
		}
	    }
	});

        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
	    if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
		if ((state & WAIT_FLG) != WAIT_FLG) {
		    return;
		}

		try (MemoryStack stack = stackPush()) {
		    final DoubleBuffer xptr = stack.mallocDouble(1);
		    final DoubleBuffer yptr = stack.mallocDouble(1);
		    glfwGetCursorPos(window, xptr, yptr);

		    final double x = xptr.get(0);
		    final double y = yptr.get(0);
		    if (!(x <= 20 || x >= 280 || y <= 20 || y >= 280)) {
			mouseVirtX = Math.min((int) ((x - 20) / 28), Board.DIMENSION - 1);
			mouseVirtY = Math.min((int) ((y - 20) / 28), Board.DIMENSION - 1);
			state ^= WAIT_FLG;
		    }
		}
	    }
	});

        final GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(window,
			 (vidmode.width() - WIDTH) / 2,
			 (vidmode.height() - HEIGHT) / 2);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(GLFW_TRUE);
        glfwShowWindow(window);
    }

    private void loop() {
        GL.createCapabilities();
        glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            update();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            render();
            glfwSwapBuffers(window);
        }
    }

    private void update() {
	if (state == INITING) {
	    board.reset();
	    p1RemSpawns = p2RemSpawns = 5;
	    startTime = System.currentTimeMillis();
	    
	    // Populate vboIdBoard
	    try (MemoryStack stack = stackPush()) {
		final FloatBuffer buf = stack.mallocFloat(136);
		// Horizontal lines
		buf.put(-0.8f).put(0.8f);
		buf.put(0.8f).put(0.8f);
		buf.put(-0.8f).put(0.6f);
		buf.put(0.8f).put(0.6f);
		buf.put(-0.8f).put(0.4f);
		buf.put(0.8f).put(0.4f);
		buf.put(-0.8f).put(0.2f);
		buf.put(0.8f).put(0.2f);
		buf.put(-0.8f).put(0.0f);
		buf.put(-0.4f).put(0.0f);
		buf.put(0.4f).put(0.0f);
		buf.put(0.8f).put(0.0f);
		buf.put(-0.8f).put(-0.2f);
		buf.put(0.8f).put(-0.2f);
		buf.put(-0.8f).put(-0.4f);
		buf.put(0.8f).put(-0.4f);
		buf.put(-0.8f).put(-0.6f);
		buf.put(0.8f).put(-0.6f);
		buf.put(-0.8f).put(-0.8f);
		buf.put(0.8f).put(-0.8f);

		// Vertical lines
		buf.put(-0.8f).put(-0.8f);
		buf.put(-0.8f).put(0.8f);
		buf.put(-0.6f).put(-0.8f);
		buf.put(-0.6f).put(0.8f);
		buf.put(-0.4f).put(-0.8f);
		buf.put(-0.4f).put(0.8f);
		buf.put(-0.2f).put(-0.8f);
		buf.put(-0.2f).put(-0.2f);
		buf.put(-0.2f).put(0.2f);
		buf.put(-0.2f).put(0.8f);
		buf.put(0.0f).put(-0.8f);
		buf.put(0.0f).put(-0.2f);
		buf.put(0.0f).put(0.2f);
		buf.put(0.0f).put(0.8f);
		buf.put(0.2f).put(-0.8f);
		buf.put(0.2f).put(-0.2f);
		buf.put(0.2f).put(0.2f);
		buf.put(0.2f).put(0.8f);
		buf.put(0.4f).put(-0.8f);
		buf.put(0.4f).put(0.8f);
		buf.put(0.6f).put(-0.8f);
		buf.put(0.6f).put(0.8f);
		buf.put(0.8f).put(-0.8f);
		buf.put(0.8f).put(0.8f);

		// Diagonal lines
		buf.put(-0.8f).put(-0.6f);
		buf.put(-0.4f).put(-0.2f);
		buf.put(-0.6f).put(-0.8f);
		buf.put(0.0f).put(-0.2f);
		buf.put(0.2f).put(-0.8f);
		buf.put(0.8f).put(-0.2f);

		buf.put(0.8f).put(-0.6f);
		buf.put(0.4f).put(-0.2f);
		buf.put(0.6f).put(-0.8f);
		buf.put(0.0f).put(-0.2f);
		buf.put(-0.2f).put(-0.8f);
		buf.put(-0.8f).put(-0.2f);

		buf.put(-0.8f).put(0.6f);
		buf.put(-0.4f).put(0.2f);
		buf.put(-0.6f).put(0.8f);
		buf.put(0.0f).put(0.2f);
		buf.put(0.2f).put(0.8f);
		buf.put(0.8f).put(0.2f);

		buf.put(0.8f).put(0.6f);
		buf.put(0.4f).put(0.2f);
		buf.put(0.6f).put(0.8f);
		buf.put(0.0f).put(0.2f);
		buf.put(-0.2f).put(0.8f);
		buf.put(-0.8f).put(0.2f);
		buf.flip();

		vaoIdBoard = glGenVertexArrays();
		glBindVertexArray(vaoIdBoard);

		vboIdBoard = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboIdBoard);
		glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindVertexArray(0);
	    }

	    state = P1_TURN0 | WAIT_FLG;
	    return;
	}
	
        switch (board.getWinner()) {
	case 1:
	case 2:
	    state = INITING; // game restarts
	    return;
        }

        switch (state) {
	case P1_TURN0:
	    if (board.hasP1Piece(mouseVirtX, mouseVirtY)) {
		originX = mouseVirtX;
		originY = mouseVirtY;
		state = P1_TURN1;
	    } else if (p1RemSpawns > 0
		       && board.trySpawnP1At(mouseVirtX, mouseVirtY)) {
		logGame("P1 spawn");
		p1RemSpawns--;
		state = P2_TURN0;
	    }
	    state |= WAIT_FLG;
	    return;
	case P1_TURN1: {
	    if (originX == mouseVirtX && originY == mouseVirtY) {
		state = P1_TURN0 | WAIT_FLG;
		return;
	    }
		
	    final int[] validTiles = board.getMovementOptions(originX, originY, 1);
	    for (int i = 0; i < validTiles.length; i += 2) {
		if (mouseVirtX == validTiles[i] && mouseVirtY == validTiles[i + 1]) {
		    logGame("P1 " + originX + "" + (char) ('a' + originY)
			    + "-" + mouseVirtX + "" + (char) ('a' + mouseVirtY));
		    board.bruteMove(originX, originY, mouseVirtX, mouseVirtY);
		    state = P2_TURN0 | WAIT_FLG;
		    return;
		}
	    }
	    state |= WAIT_FLG;
	    return;
	}
	case P2_TURN0:
	    if (board.hasP2Piece(mouseVirtX, mouseVirtY)) {
		originX = mouseVirtX;
		originY = mouseVirtY;
		state = P2_TURN1;
	    } else if (p2RemSpawns > 0
		       && board.trySpawnP2At(mouseVirtX, mouseVirtY)) {
		logGame("P2 spawn");
		p2RemSpawns--;
		state = P1_TURN0;
	    }
	    state |= WAIT_FLG;
	    return;
	case P2_TURN1: {
	    if (originX == mouseVirtX && originY == mouseVirtY) {
		state = P2_TURN0 | WAIT_FLG;
		return;
	    }

	    final int[] validTiles = board.getMovementOptions(originX, originY, 2);
	    for (int i = 0; i < validTiles.length; i += 2) {
		if (mouseVirtX == validTiles[i] && mouseVirtY == validTiles[i + 1]) {
		    logGame("P2 " + originX + "" + (char) ('a' + originY)
			    + "-" + mouseVirtX + "" + (char) ('a' + mouseVirtY));
		    board.bruteMove(originX, originY, mouseVirtX, mouseVirtY);
		    state = P1_TURN0 | WAIT_FLG;
		    return;
		}
	    }

	    state |= WAIT_FLG;
	    return;
	}
        }
    }

    private void render() {
        glColor3f(0.5f, 0.5f, 0.5f);
        glBindVertexArray(vaoIdBoard);
        glEnableVertexAttribArray(0);
        glDrawArrays(GL_LINES, 0, 68);

        glDisableVertexAttribArray(0);
        glBindVertexArray(0);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBegin(GL_QUADS);
        float startX = -0.8f;
        for (int i = 0; i < Board.DIMENSION; ++i) {
            float startY = -0.8f;
            // Draw the board bottom-up (board is top-down)
            for (int j = Board.DIMENSION - 1; j >= 0; --j) {
                if (board.hasP1Piece(i, j)) {
                    if (originX == i && originY == j
			&& (state & DEST_FLG) == DEST_FLG) {
                        glColor4f(1.0f, 0.0f, 0.0f, 0.25f);
                    } else {
                        glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
                    }
                    glVertex2f(startX - 0.075f, startY + 0.075f);
                    glVertex2f(startX - 0.075f, startY - 0.075f);
                    glVertex2f(startX + 0.075f, startY - 0.075f);
                    glVertex2f(startX + 0.075f, startY + 0.075f);
                } else if (board.hasP2Piece(i, j)) {
                    if (originX == i && originY == j
			&& (state & DEST_FLG) == DEST_FLG) {
                        glColor4f(0.0f, 0.5f, 1.0f, 0.25f);
                    } else {
                        glColor4f(0.0f, 0.5f, 1.0f, 1.0f);
                    }
                    glVertex2f(startX - 0.075f, startY + 0.075f);
                    glVertex2f(startX - 0.075f, startY - 0.075f);
                    glVertex2f(startX + 0.075f, startY - 0.075f);
                    glVertex2f(startX + 0.075f, startY + 0.075f);
                }
                startY += 0.2f;
            }
            startX += 0.2f;
        }
        glEnd();
        glDisable(GL_BLEND);
    }

    private void destroy() {
        glDisableVertexAttribArray(0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboIdBoard);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoIdBoard);
    }

    private void logGame(String msg) {
	System.err.println("AT " + (System.currentTimeMillis() - startTime) / 1000
			   + "s\t" + msg);
    }

    public static void main(String[] args) {
        new App().run();
    }
}
