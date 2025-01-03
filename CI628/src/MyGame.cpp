#include "MyGame.h"
#include "SDL_ttf.h"

TTF_Font* font = TTF_OpenFont("assets/arial.ttf", 22);
static SDL_Color TEXT_COLOUR = { 255, 255, 255 }; // Text Colour is set to white.
SDL_Surface* textSurface = nullptr;
SDL_Texture* textTexture = nullptr;
std::string player1ScoreTextString;
std::string player2ScoreTextString;
bool xray = false;


void MyGame::on_receive(std::string cmd, std::vector<std::string>& args) {
    if (cmd == "GAME_DATA") {
        // Seperates data into different variables such as game object position and score.
        if (args.size() == 9) {
            game_data.player1Y = stoi(args.at(0));
            game_data.player2Y = stoi(args.at(1));
            game_data.ballX = stoi(args.at(2));
            game_data.ballY = stoi(args.at(3));
            game_data.player1X = stoi(args.at(4));
            game_data.player2X = stoi(args.at(5));
            game_data.connectionID = stoi(args.at(6));
            game_data.player1Score = stoi(args.at(7));
            game_data.player2Score = stoi(args.at(8));
        }
    } else {
        player1.y = game_data.player1Y;
        player2.y = game_data.player2Y;
        ball.x = game_data.ballX;
        ball.y = game_data.ballY;
        player1.x = game_data.player1X;
        player2.x = game_data.player2X;
    }
}

void MyGame::send(std::string message) {
    messages.push_back(message);
}

void MyGame::input(SDL_Event& event) {
    switch (event.key.keysym.sym) {
        case SDLK_w:
            send(event.type == SDL_KEYDOWN ? "W_DOWN" : "W_UP");
            game_data.moveUp = true;
            if (event.type == SDL_KEYUP) game_data.moveUp = false;
            break;

        case SDLK_s:
            send(event.type == SDL_KEYDOWN ? "S_DOWN" : "S_UP");
            game_data.moveDown = true;
            if (event.type == SDL_KEYUP) game_data.moveDown = false;
            break;

            // When X key is pressed, xray mode will activate. When released xray will be false.
        case SDLK_x:
            xray = true;
            if (event.type == SDL_KEYUP) xray = false;
            break;
    }
}

void MyGame::updateGUI(SDL_Renderer* renderer) 
{
    SDL_Rect textRect = { 64,8,0,0 };
    std::string screenText = "Player 1: " + std::to_string(game_data.player1Score);
    
    textSurface = TTF_RenderText_Blended_Wrapped(font, screenText.c_str(), TEXT_COLOUR, 128);
    textTexture = SDL_CreateTextureFromSurface(renderer, textSurface);
    if (!textSurface) {
        std::cerr << "Failed to create text surface: " << TTF_GetError() << std::endl;
    }
    if (!textTexture) {
        std::cerr << "Failed to create text texture: " << TTF_GetError() << std::endl;
    }
    SDL_QueryTexture(textTexture, NULL, NULL, &textRect.w, &textRect.h);
    SDL_RenderCopy(renderer, textTexture, NULL, &textRect);

    SDL_FreeSurface(textSurface);
    SDL_DestroyTexture(textTexture);

    textRect = { 624,8,0,0 };
    screenText = "Player 2: " + std::to_string(game_data.player2Score);

    textSurface = TTF_RenderText_Blended_Wrapped(font, screenText.c_str(), TEXT_COLOUR, 128);
    textTexture = SDL_CreateTextureFromSurface(renderer, textSurface);
    if (!textSurface) {
        std::cerr << "Failed to create text surface: " << TTF_GetError() << std::endl;
    }
    if (!textTexture) {
        std::cerr << "Failed to create text texture: " << TTF_GetError() << std::endl;
    }
    SDL_QueryTexture(textTexture, NULL, NULL, &textRect.w, &textRect.h);
    SDL_RenderCopy(renderer, textTexture, NULL, &textRect);

    SDL_FreeSurface(textSurface);
    SDL_DestroyTexture(textTexture);

}
void MyGame::playerMovement()
{
    if (game_data.moveDown == true)
        if (game_data.connectionID == 1)
            player1.y -= 1 * game_data.PLAYER_SPEED;
    if (game_data.moveUp == true)
        if (game_data.connectionID == 1)
            player1.y += 1 * game_data.PLAYER_SPEED;

    if (game_data.moveDown == true)
        if (game_data.connectionID == 2)
            player2.y -= 1 * game_data.PLAYER_SPEED;
    if (game_data.moveUp == true)
        if (game_data.connectionID == 2)
            player2.y += 1 * game_data.PLAYER_SPEED;
}

void MyGame::loadTextures(SDL_Renderer* renderer) {
    // Load player textures
    SDL_Surface* tempSurface = IMG_Load("assets/player1.png");
    if (!tempSurface) {
        std::cerr << "Failed to load player1.png: " << IMG_GetError() << std::endl;
    }
    else {
        game_data.player1Texture = SDL_CreateTextureFromSurface(renderer, tempSurface);
        SDL_FreeSurface(tempSurface);
    }

    tempSurface = IMG_Load("assets/player2.png");
    if (!tempSurface) {
        std::cerr << "Failed to load player2.png: " << IMG_GetError() << std::endl;
    }
    else {
        game_data.player2Texture = SDL_CreateTextureFromSurface(renderer, tempSurface);
        SDL_FreeSurface(tempSurface);
    }

    // Load ball texture
    tempSurface = IMG_Load("assets/ball.png");
    if (!tempSurface) {
        std::cerr << "Failed to load ball.png: " << IMG_GetError() << std::endl;
    }
    else {
        game_data.ballTexture = SDL_CreateTextureFromSurface(renderer, tempSurface);
        SDL_FreeSurface(tempSurface);
    }

    tempSurface = IMG_Load("assets/background.png");
    if (!tempSurface) {
        std::cerr << "Failed to load background.png: " << IMG_GetError() << std::endl;
    }
    else {
        game_data.backgroundTexture = SDL_CreateTextureFromSurface(renderer, tempSurface);
        SDL_FreeSurface(tempSurface);
    }
    // Initialize font and error handling
    TTF_Init();
    font = TTF_OpenFont("assets/arial.ttf", 22);
    if (!font) {
        std::cerr << "Failed to load font: " << TTF_GetError() << std::endl;
    }
}

void MyGame::destroyTextures() {
    if (game_data.player1Texture) SDL_DestroyTexture(game_data.player1Texture);
    if (game_data.player2Texture) SDL_DestroyTexture(game_data.player2Texture);
    if (game_data.ballTexture) SDL_DestroyTexture(game_data.ballTexture);
    if (game_data.backgroundTexture) SDL_DestroyTexture(game_data.backgroundTexture);
}

// Runs every frame
void MyGame::update() 
{
    playerMovement();
}

void MyGame::render(SDL_Renderer* renderer) {
    if (xray) 
    {
        SDL_RenderDrawRect(renderer, &ball);
        SDL_RenderDrawRect(renderer, &player2);
        SDL_RenderDrawRect(renderer, &player1);
    }
    else if (!xray) 
    {
        if (game_data.backgroundTexture) {
            SDL_Rect backgroundRect = { 0, 0, 800, 600 };
            SDL_RenderCopy(renderer, game_data.backgroundTexture, nullptr, &backgroundRect);
        }
        if (game_data.player1Texture) {
            SDL_RenderCopy(renderer, game_data.player1Texture, nullptr, &player1);
        }
        if (game_data.player2Texture) {
            SDL_RenderCopy(renderer, game_data.player2Texture, nullptr, &player2);
        }
        if (game_data.ballTexture) {
            SDL_RenderCopy(renderer, game_data.ballTexture, nullptr, &ball);
        }
       
    }
    SDL_SetRenderDrawColor(renderer, 255, 255, 255, 255);
    updateGUI(renderer);
}