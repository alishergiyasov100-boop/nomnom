# NomNom 🍴

Android-app: снимаешь блюдо → Qwen-Vision оценивает калории и БЖУ → дневной счётчик + аппетитные комменты.

## Стек
Kotlin · Compose · Material3 · DataStore · Coil · OkHttp · Coroutines + Serialization.

## API
OpenAI-compat `/chat/completions` с моделью `qwen3-vl-plus` через Luna-Proxy
(`http://127.0.0.1:8765/v1` по умолчанию — Luna слушает на этом порту в Termux).

Base URL, модель и API-key редактируются в Настройках.

## Сборка
- GitHub Actions: `.github/workflows/build.yml` собирает debug + release APK при пуше в `main`.
- Локально (если есть Android SDK): `./gradlew assembleDebug`.

## Запуск Luna
```
cd ~/projects/luna-proxy
PORT=8765 PUPPETEER_SKIP_DOWNLOAD=true \
  nohup ./node_modules/.bin/ts-node-dev --transpile-only src/dev.ts \
  > logs/luna.log 2>&1 & disown
```
