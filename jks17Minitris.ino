#include <AberLED.h>  // Include the library for controlling the AberLED display.

// Define constants for different game states
#define S_INVALID -1  // Represents an invalid state.
#define S_START 0     // The state when the game is starting.
#define S_PLAYING 1   // The state when the game is being played.
#define S_PAUSED 2    // The state when the game is paused.
#define S_END 3       // The state when the game has ended.

int state = S_INVALID;         // The current state of the game, initialized to invalid state.
unsigned long stateStartTime;  // The time when the current state was entered.

const int rows = 8;                          // Number of rows in the game grid.
const int cols = 8;                          // Number of columns in the game grid.
int LVL, linesCleared, printed, Multiplier = 0;  // Variables to track the game level, number of lines cleared, the final print and the multiplier.
long score;                                  // Variable to track the player's score.

// Function to change the current game state
void gotoState(int s) {
  Serial.print("Going to state ");  // Print the state transition for debugging.
  Serial.println(s);
  state = s;                  // Set the new state.
  stateStartTime = millis();  // Record the time the state was entered.
}

// Function to get the time the system has been in the current state
unsigned long getStateTime() {
  return millis() - stateStartTime;  // Return the elapsed time since the current state was entered.
}

// Declare the 8x8 game grid and initialize it with zeros.
int gameGrid[rows][cols] = {};  // Initialize to all zeros.

// Function to initialize the game grid with all zeros.
void initializeGame(int arr[rows][cols]) {
  for (int i = 0; i < rows; ++i) {
    for (int j = 0; j < cols; ++j) {
      arr[i][j] = 0;  // Set each element to 0.
    }
  }
  //Reset/initalise game variables each time a new game starts.
  score = 0;         // Initialize the score .
  LVL = 1;           // Set the initial level to 1.
  linesCleared = 0;  // Initialize the number of lines cleared
  Multiplier = 1;    // Initilise the speed multiplier.
}

// Function to print the current game grid to the Serial Monitor for debugging.
void printgameGrid(int arr[rows][cols]) {
  for (int i = 0; i < rows; ++i) {
    for (int j = 0; j < cols; ++j) {
      Serial.print(arr[j][i]);  // Print the value at each grid position.
      Serial.print("\t");       // Add a tab space between values for better formatting.
    }
    Serial.println();  // Print a newline after each row.
  }
}

// Variables to track the player's position.
int playerX, playerY;
long lastupdate;
long updateInterval = 500;  // Interval between updates for player movement (in milliseconds).

// Function to spawn a new player block at the top of the grid.
void newPlayerBlock() {
  lastupdate = getStateTime();  // Record the time of the state change.
  playerX = random(1, 8);       // Set the player's X position randomly between 1 and 7.
  playerY = 0;                  // Set the player's Y position to 0 (the top row).
  score += 100;                 // Increase the score by 100 for every new block.

  if (gameGrid[playerX][playerY] == 1) {  // Check if the starting position is already occupied.
    gotoState(S_END);// If occupied, the game ends.
    printed=0;                     
  }
}

// Function to move the player block to the left.
void movePlayerLeft() {
  if (playerX - 1 >= 0 && gameGrid[playerX - 1][playerY] != 1) {  // Check if the move is within bounds and not blocked.
    playerX -= 1;                                                 // Move the player to the left.
  }
}

// Function to move the player block to the right.
void movePlayerRight() {
  if (playerX + 1 <= 7 && gameGrid[playerX + 1][playerY] != 1) {  // Check if the move is within bounds and not blocked.
    playerX += 1;                                                 // Move the player to the right.
  }
}

// Function to make the player block fall until it reaches the bottom or a filled space.
void fullDrop() {
  int bottom = 0;
  for (int j = 0; j < cols; j++) {
    Serial.println();
    if ((gameGrid[playerX][j + 1] == 1 || j >= cols - 1) && bottom == 0) {  // Check if the block has reached the bottom.
      gameGrid[playerX][j] = 1;                                             // Place the block on the grid.
      checkLine();                                                          // Check if any line is full.
      newPlayerBlock();                                                     // Spawn a new player block.
      break;
    }
  }
}

// Function to shift all rows Up when a line is Duplicated.
void duplicateBottom() {
  for (int i = 0; i <= rows; ++i) {         // Iterate through all rows.
    for (int j = 1; j <= cols - 1; ++j) {   // Iterate through each column in the row.
      gameGrid[i][j - 1] = gameGrid[i][j];  // Copy's the value to the space above.
    }
  }
}

// Function to scroll rows down after a line is cleared.
void scrollRowsAbove(int y) {

  // Shift all rows down by one.
  for (int i = 0; i <= rows; ++i) {
    for (int j = y; j > 0; --j) {
      gameGrid[i][j] = gameGrid[i][j - 1];  // Move the above row's column value down.
    }
  }

  // Zero out the top-most row.
  for (int i = 0; i < rows; ++i) {
    gameGrid[i][0] = 0;  // Set all values in the top row to 0.
  }
  linesCleared++;  // Increment the linesCleared counter.
}

// Function to check if a line is completely filled.
void checkLine() {
  int full = 1;  // Flag to check if the line is full.
  for (int i = 0; i < cols; i++) {
    if (gameGrid[i][7] == 0) {  // If any space in the last row is empty, the line is not full.
      full = 0;
    }
  }
  if (full == 1) {
    scrollRowsAbove(7);  // If the line is full, scroll all rows above it.
  }
}

// Function to update the player's position.
void updatePlayer() {
  lastupdate = getStateTime();                                    // Update the last update time.
  if (gameGrid[playerX][playerY + 1] == 1 || playerY + 1 >= 8) {  // Check if the player block has reached the bottom or another block.
    gameGrid[playerX][playerY] = 1;                               // Place the block in the grid.
    checkLine();                                                  // Check for any cleared lines.
    newPlayerBlock();                                             // Spawn a new player block.
  }
  if (linesCleared % 2 == 0 & linesCleared != 0) {  // Increase the level after every 2 lines cleared.
    LVL = linesCleared / 2;
    Multiplier += 0.1;
  }
  playerY += 1;  // Move the player block down.
}

// Function to update the game model based on the current state.
void updateModel() {
  switch (state) {
    case S_START:
      break;
    case S_PLAYING:
      // Update player position based on the level (difficulty increases as the level increases).
      if (getStateTime() - lastupdate > updateInterval * Multiplier) {
        updatePlayer();  // Update player block position.
        int i = random(1, 6);
        if (i == 2) {
          duplicateBottom();  // Occasionally call duplicateBottom for extra block movement.
        }
      }
      break;
    case S_PAUSED:
      break;
    case S_END:
      break;
  }
}

// Function to handle user inputs (e.g., button presses).
void handleInput() {
  switch (state) {
    case S_START:
      if (AberLED.getButtonDown(FIRE)) {  // Start the game when the FIRE button is pressed.
        gotoState(S_PLAYING);
      }
      break;
    case S_PLAYING:
      if (AberLED.getButtonDown(FIRE)) {  // Pause the game when the FIRE button is pressed.
        gotoState(S_PAUSED);
      }
      if (AberLED.getButtonDown(LEFT)) {  // Move the player block left.
        movePlayerLeft();
      }
      if (AberLED.getButtonDown(RIGHT)) {  // Move the player block right.
        movePlayerRight();
      }
      if (AberLED.getButtonDown(DOWN)) {  // Drop the player block to the bottom immediately.
        fullDrop();
      }
      break;
    case S_PAUSED:
      if (AberLED.getButtonDown(FIRE)) {  // Resume the game after a pause when the FIRE button is pressed.
        if (getStateTime() >= 1000) {
          gotoState(S_PLAYING);
        }
      }
    case S_END:
      if (AberLED.getButtonDown(FIRE)) {  // Restart the game after it ends when the FIRE button is pressed.
        if (getStateTime() >= 1000) {
          gotoState(S_START);
        }
      }
  }
}

// Function to render the end game screen.
void renderEND() {

  if (printed==0) {
    AberLED.addToText("END");
    Serial.print("Score: ");
    Serial.println(score * LVL);  // Display the final score.
    Serial.print("LVL REACHED: ");
    Serial.println(LVL);  // Display the final level reached.
    printed = 1;
  }
}

// Function to render the player's current position on the screen.
void renderPlayer() {
  AberLED.set(playerX, playerY, YELLOW);  // Render the player block in yellow.
}

// Function to render the blocks on the game grid.
void renderBlock() {
  for (int i = 0; i < rows; ++i) {
    for (int j = 0; j < cols; ++j) {
      if (gameGrid[i][j] == 1) {  // If a block is placed at the position, render it in red.
        AberLED.set(i, j, RED);
      }
    }
  }
}

// Function to render the game screen based on the current state.
void render() {
  switch (state) {
    case S_START:
      AberLED.addToText("start");
      initializeGame(gameGrid);  // Initialize the game when starting.
      break;
    case S_PLAYING:
      AberLED.addToText("playing");
      renderPlayer();  // Render the player.
      renderBlock();   // Render the blocks.
      break;
    case S_PAUSED:
      renderPlayer();  // Render the player.
      renderBlock();   // Render the blocks.
      AberLED.addToText("Paused");
      break;
    case S_END:
      renderEND();  // Render the end screen.
      break;
  }
}

// Setup function to initialize the game and serial communication.
void setup() {
  AberLED.begin();     // Initialize the AberLED library.
  Serial.begin(9600);  // Start serial communication.
  gotoState(S_START);  // Start the game in the START state.
}

// Main loop function to repeatedly handle inputs, update the game state, and render the screen.
void loop() {
  handleInput();        // Handle user inputs (button presses).
  updateModel();        // Update the game model (game logic).
  AberLED.clear();      // Clear the screen.
  AberLED.clearText();  // Clear any text on the screen.
  render();             // Render the current game state.
  AberLED.swap();       // Swap the display buffer to show the updated screen.
}
