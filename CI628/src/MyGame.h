#ifndef __MY_GAME_H__
#define __MY_GAME_H__

#include <iostream>
#include <vector>
#include <string>

#include "SDL.h"
#include "SDL_image.h"

static struct GameData {
    int player1Y = 0;
    int player2Y = 0;
    int ballX = 0;
    int ballY = 0;
    int player1X = 0;
    int player2X = 0;
    const double PLAYER_SPEED = 15;
    bool moveDown;
    bool moveUp;
    int playerID;
    int connectionID;
    int player1Score;
    int player2Score;

    SDL_Texture* player1Texture = nullptr;
    SDL_Texture* player2Texture = nullptr;
    SDL_Texture* ballTexture = nullptr;
    SDL_Texture* backgroundTexture = nullptr;

} game_data;

class MyGame {

    private:
        SDL_Rect player1 = { 200, 0, 20, 60 };
        SDL_Rect player2 = { 580, 0, 20, 60 };
        SDL_Rect ball = { 390, 0, 20, 20 };

    public:
        std::vector<std::string> messages;
        
        void playerMovement();
        std::string xor_encrypt_decrypt(const std::string& data, char key);

        void on_receive(std::string message, std::vector<std::string>& args);
        void send(std::string message);
        void input(SDL_Event& event);
        void update();
        void loadTextures(SDL_Renderer* renderer);
        void updateGUI(SDL_Renderer* renderer);
        void destroyTextures();
        void render(SDL_Renderer* renderer);

};

#endif