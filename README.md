# Alarme 3x3 (Android Nativo — Kotlin + Jetpack Compose)

Aplicativo Android para automação de alarmes em escala contínua **3x3** (3 dias de trabalho por 3 dias de folga), com persistência offline (Room + DataStore) e agendamento nativo via `AlarmManager.setAlarmClock()`.

## Como gerar o `.apk` instalável

### Opção 1: Compilar na nuvem pelo GitHub Actions (sem instalar Android Studio)
1. Crie um repositório no GitHub (público ou privado).
2. Extraia este ZIP e envie todo o conteúdo da pasta `Alarme3x3/` para a raiz do repositório (incluindo a pasta oculta `.github/workflows/build-apk.yml`).
3. Acesse a aba **Actions** no repositório do GitHub.
4. O workflow **Build Android APK** será executado automaticamente (ou clique em **Run workflow**).
5. Ao concluir, baixe o artefato **`Alarme3x3-debug-apk`** no final da página do workflow, extraia o `app-debug.apk` e instale no Android.

### Opção 2: Compilar localmente no Android Studio
1. Extraia o ZIP e abra a pasta `Alarme3x3` no **Android Studio** (JDK 17 / AGP 8.7.2).
2. Aguarde o **Gradle Sync** baixar as dependências.
3. No menu superior, acesse **Build -> Build Bundle(s) / APK(s) -> Build APK(s)**.
4. O arquivo será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Observação sobre permissões no Android
Após instalar o APK no aparelho, abra o app e conceda:
- **Notificações** (Android 13+)
- **Alarmes e Lembretes / Alarmes Exatos** (Android 12+)
- **Tela cheia na tela de bloqueio** (Android 14+)
- **Sem restrições de bateria** (recomendado em aparelhos Samsung, Xiaomi, Motorola e Realme)
