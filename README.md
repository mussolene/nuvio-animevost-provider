# AnimeVost для Nuvio

[![Проверка сборки](https://github.com/mussolene/nuvio-animevost-provider/actions/workflows/build.yml/badge.svg)](https://github.com/mussolene/nuvio-animevost-provider/actions/workflows/build.yml)
[![Лицензия GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)

Изолированный CloudStream-провайдер AnimeVost для Nuvio на Android TV. Он добавляет
поиск аниме, карточки, серии и прямые видео второго проигрывателя AnimeVost. Провайдер
передает 480p, 720p и 1080p, когда эти варианты присутствуют в проигрывателе источника.

## Установка

1. Откройте в Nuvio раздел `Настройки -> Контент и поиск -> Плагины`.
2. Добавьте адрес репозитория:

   ```text
   https://raw.githubusercontent.com/mussolene/nuvio-animevost-provider/main/dist/plugins.json
   ```

3. Обновите репозиторий и включите AnimeVost.

После установки компьютер или отдельный сервер не нужен. Nuvio загружает готовый
пакет и последующие версии напрямую из GitHub.

## Сборка и проверка

Требуются JDK 17 и Android SDK.

```sh
./gradlew --no-daemon -Pkotlin.incremental=false \
  :AnimeVostNuvio:testDebugUnitTest \
  :AnimeVostNuvio:make
```

## Безопасность и ограничения

- Провайдер не принимает учетные данные и не хранит пользовательскую информацию.
- Запросы отправляются только AnimeVost и адресам видео, опубликованным его проигрывателем.
- Доступность карточек и потоков зависит от внешнего сайта и текущей сети.
- Проект не связан с Nuvio или владельцами AnimeVost.

Порядок сообщения об уязвимостях описан в [SECURITY.md](SECURITY.md).

## Происхождение кода и лицензия

Правила разбора каталога и серий основаны на открытом расширении AnimeVost из
`yuzono/anime-extensions` под лицензией Apache-2.0 и поведении приложения
`unixway/AnimeVostTV` под лицензией BSD-2-Clause. Код этого репозитория
распространяется по лицензии [GPL-3.0](LICENSE).
