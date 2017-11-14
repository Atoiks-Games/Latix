package org.atoiks.latix;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class App {

    private static final int HEIGHT = 300;
    private static final int WIDTH = 300;

    private static final byte INITING =  0b0000;
    private static final byte WAIT_FLG = 0b0001;
    private static final byte DEST_FLG = 0b1000;
    private static final byte P1_FLG =   0b0010;
    private static final byte P2_FLG =   0b0100;
    
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

    public void run() {
	init();
	loop();

	glfwFreeCallbacks(window);
	glfwDestroyWindow(window);

	glfwTerminate();
	glfwSetErrorCallback(null).free();
    }

    public void init() {
	GLFWErrorCallback.createPrint(System.err).set();
	if (!glfwInit()) throw new IllegalStateException("Cannot init GLFW");

	glfwDefaultWindowHints();
	glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
	glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

	window = glfwCreateWindow(300, 300, "Chess?", NULL, NULL);
	if (window == NULL) throw new RuntimeException("Failed to crete GLFW window");

	glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
		if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
		    if ((state & WAIT_FLG) != WAIT_FLG) return;
		    
		    try (MemoryStack stack = stackPush()) {
			final DoubleBuffer xptr = stack.mallocDouble(1);
			final DoubleBuffer yptr = stack.mallocDouble(1);
			glfwGetCursorPos(window, xptr, yptr);

			final double x = xptr.get(0);
			final double y = yptr.get(0);
			if (!(x <= 20 || x >= 280 || y <= 20 || y >= 280)) {
			    mouseVirtX = (int) ((x - 20) / 28);
			    mouseVirtY = (int) ((y - 20) / 28);
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

	board.reset();
    }

    private void loop() {
	GL.createCapabilities();
	glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

	state = P1_TURN0 | WAIT_FLG;

	while (!glfwWindowShouldClose(window)) {
	    glfwPollEvents();
	    update();
	    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	    render();
	    glfwSwapBuffers(window);
	}
    }

    private void update() {
        switch (board.getWinner()) {
	case 1:
	case 2:
	    glfwSetWindowShouldClose(window, true);
	    return;
	}

	switch (state) {
	case P1_TURN0:
	    if (board.hasP1Piece(mouseVirtX, mouseVirtY)) {
		originX = mouseVirtX;
		originY = mouseVirtY;
		state = P1_TURN1;
	    }
	    state |= WAIT_FLG;
	    return;
	case P1_TURN1: {
	    final int[] validTiles = board.getMovementOptions(originX, originY, 1);
	    for (int i = 0; i < validTiles.length; i += 2) {
		if (mouseVirtX == validTiles[i] && mouseVirtY == validTiles[i + 1]) {
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
	    }
	    state |= WAIT_FLG;
	    return;
	case P2_TURN1: {
	    final int[] validTiles = board.getMovementOptions(originX, originY, 2);
	    for (int i = 0; i < validTiles.length; i += 2) {
		if (mouseVirtX == validTiles[i] && mouseVirtY == validTiles[i + 1]) {
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
	glBegin(GL_LINES);
	{
	    // Horizontal lines
	    glVertex2f(-0.8f, 0.8f);
	    glVertex2f(0.8f, 0.8f);
	    glVertex2f(-0.8f, 0.6f);
	    glVertex2f(0.8f, 0.6f);
	    glVertex2f(-0.8f, 0.4f);
	    glVertex2f(0.8f, 0.4f);
	    glVertex2f(-0.8f, 0.2f);
	    glVertex2f(0.8f, 0.2f);
	    glVertex2f(-0.8f, 0.0f);
	    glVertex2f(-0.4f, 0.0f);
	    glVertex2f(0.4f, 0.0f);
	    glVertex2f(0.8f, 0.0f);
	    glVertex2f(-0.8f, -0.2f);
	    glVertex2f(0.8f, -0.2f);
	    glVertex2f(-0.8f, -0.4f);
	    glVertex2f(0.8f, -0.4f);
	    glVertex2f(-0.8f, -0.6f);
	    glVertex2f(0.8f, -0.6f);
	    glVertex2f(-0.8f, -0.8f);
	    glVertex2f(0.8f, -0.8f);

	    // Vertical lines
	    glVertex2f(-0.8f, -0.8f);
	    glVertex2f(-0.8f, 0.8f);
	    glVertex2f(-0.6f, -0.8f);
	    glVertex2f(-0.6f, 0.8f);
	    glVertex2f(-0.4f, -0.8f);
	    glVertex2f(-0.4f, 0.8f);
	    glVertex2f(-0.2f, -0.8f);
	    glVertex2f(-0.2f, -0.2f);
	    glVertex2f(-0.2f, 0.2f);
	    glVertex2f(-0.2f, 0.8f);
	    glVertex2f(0.0f, -0.8f);
	    glVertex2f(0.0f, -0.2f);
	    glVertex2f(0.0f, 0.2f);
	    glVertex2f(0.0f, 0.8f);
	    glVertex2f(0.2f, -0.8f);
	    glVertex2f(0.2f, -0.2f);
	    glVertex2f(0.2f, 0.2f);
	    glVertex2f(0.2f, 0.8f);
	    glVertex2f(0.4f, -0.8f);
	    glVertex2f(0.4f, 0.8f);
	    glVertex2f(0.6f, -0.8f);
	    glVertex2f(0.6f, 0.8f);
	    glVertex2f(0.8f, -0.8f);
	    glVertex2f(0.8f, 0.8f);

	    // Diagonal lines
	    glVertex2f(-0.8f, -0.6f);
	    glVertex2f(-0.4f, -0.2f);
	    glVertex2f(-0.6f, -0.8f);
	    glVertex2f(0.0f, -0.2f);
	    glVertex2f(0.2f, -0.8f);
	    glVertex2f(0.8f, -0.2f);
	    
	    glVertex2f(0.8f, -0.6f);
	    glVertex2f(0.4f, -0.2f);
	    glVertex2f(0.6f, -0.8f);
	    glVertex2f(0.0f, -0.2f);
	    glVertex2f(-0.2f, -0.8f);
	    glVertex2f(-0.8f, -0.2f);
	    
	    glVertex2f(-0.8f, 0.6f);
	    glVertex2f(-0.4f, 0.2f);
	    glVertex2f(-0.6f, 0.8f);
	    glVertex2f(0.0f, 0.2f);
	    glVertex2f(0.2f, 0.8f);
	    glVertex2f(0.8f, 0.2f);
	    
	    glVertex2f(0.8f, 0.6f);
	    glVertex2f(0.4f, 0.2f);
	    glVertex2f(0.6f, 0.8f);
	    glVertex2f(0.0f, 0.2f);
	    glVertex2f(-0.2f, 0.8f);
	    glVertex2f(-0.8f, 0.2f);
	}
	glEnd();

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
    
    public static void main(String[] args) {
        new App().run();
    }
}
