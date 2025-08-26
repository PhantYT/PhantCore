local HttpService = game:GetService("HttpService")
local TeleportService = game:GetService("TeleportService")
local Players = game:GetService("Players")

local player = Players.LocalPlayer
local placeId = game.PlaceId
local currentJobId = game.JobId  -- Текущий JobId, чтобы исключить его

-- Функция для получения сервера с минимальным количеством игроков
local function getServerWithLeastPlayers()
    local cursor = ""
    local servers = {}
    
    repeat
        local url = "https://games.roblox.com/v1/games/" .. placeId .. "/servers/Public?sortOrder=Asc&limit=100"
        if cursor ~= "" then
            url = url .. "&cursor=" .. cursor
        end
        
        local success, response = pcall(function()
            return HttpService:GetAsync(url)
        end)
        
        if not success then
            warn("Ошибка при запросе серверов: " .. tostring(response))
            return nil
        end
        
        local data = HttpService:JSONDecode(response)
        for _, server in ipairs(data.data or {}) do
            if server.playing and server.id ~= currentJobId then  -- Исключаем текущий сервер
                table.insert(servers, {id = server.id, playing = server.playing})
            end
        end
        cursor = data.nextPageCursor or ""
    until cursor == ""
    
    if #servers == 0 then
        return nil
    end
    
    -- Сортируем по минимальному количеству игроков
    table.sort(servers, function(a, b)
        return a.playing < b.playing
    end)
    
    return servers[1].id  -- Возвращаем JobId сервера с минимумом игроков
end

-- Создание GUI
local ScreenGui = Instance.new("ScreenGui")
ScreenGui.Parent = player:WaitForChild("PlayerGui")

local Button = Instance.new("TextButton")
Button.Size = UDim2.new(0, 150, 0, 50)
Button.Position = UDim2.new(0.5, -75, 0.8, 0)
Button.Text = "Rejoin"
Button.Parent = ScreenGui

-- Обработчик клика
Button.MouseButton1Click:Connect(function()
    local serverJobId = getServerWithLeastPlayers()
    if serverJobId then
        TeleportService:TeleportToPlaceInstance(placeId, serverJobId, player)
    else
        TeleportService:Teleport(placeId, player)
    end
end)
