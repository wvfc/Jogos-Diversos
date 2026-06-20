# Jogue Comigo

**Jogue Comigo** é um aplicativo Android nativo de jogos inteligentes com
**Sudoku** e **Dama**, em que o jogador evolui de nível conforme seu
desempenho. O app funciona **offline** (máquina local) e, opcionalmente, com
**auxílio de IA externa** quando o usuário configura uma chave de API.

> 🇧🇷 Todo o aplicativo está em português do Brasil.

---

## ✨ Recursos

- **Sudoku** com tabuleiros válidos e solução única, em 4 dificuldades
  (Fácil, Médio, Difícil, Especialista). Timer, contador de erros, dicas,
  verificação de jogada, destaque de linha/coluna/bloco e validação das regras.
- **Dama** (8x8) completa: movimento diagonal, captura obrigatória, capturas
  múltiplas, promoção a dama e detecção de vitória. Motor local com **Minimax**
  e **poda alfa-beta**, com profundidade variável por dificuldade.
- **Progressão dinâmica**: sistema de XP e níveis que desbloqueiam dificuldades
  automaticamente conforme o desempenho (vitórias, tempo, erros e dicas).
- **Assistente inteligente** (interface `AiAssistant`) com duas implementações:
  - `LocalMachineAssistant` — 100% offline, baseado em regras e algoritmos.
  - `ExternalAiAssistant` — usa uma API externa **somente** quando há chave
    configurada; sem chave/Internet, recai automaticamente no modo local.
- **Perfil/Progresso**: nome do jogador, nível, XP, histórico de partidas,
  vitórias/derrotas/empates, dificuldade desbloqueada, tempo médio e dicas.
- **Configurações**: modo de jogo (Máquina ou IA), ativar/desativar dicas,
  tema claro/escuro/automático, chave de API e reset de progresso.
- Interface moderna com **Jetpack Compose** e **Material 3**, tema claro/escuro.

---

## 🧱 Tecnologias e arquitetura

- **Kotlin** + **Jetpack Compose** (UI declarativa).
- **Arquitetura MVVM** (ViewModels + `StateFlow`).
- **DataStore (Preferences)** para salvar progresso e configurações localmente.
- **kotlinx.serialization** para persistir estatísticas e histórico em JSON.
- **Gradle** (Kotlin DSL) com wrapper incluído.

### Estrutura de pastas (principais classes)

```
app/src/main/java/com/joguecomigo/
├─ MainActivity.kt              # Activity única (Compose)
├─ JogueComigoApp.kt            # Application + container de dependências
├─ domain/
│  ├─ Difficulty.kt
│  ├─ sudoku/ SudokuBoard, SudokuGenerator, SudokuSolver
│  └─ checkers/ CheckersBoard, CheckersEngine, CheckersModels
├─ data/
│  ├─ ProgressManager.kt        # persistência do progresso (DataStore)
│  ├─ SettingsManager.kt        # persistência das configurações
│  ├─ ProgressModels.kt, XpCalculator.kt
├─ ai/
│  ├─ AiAssistant.kt            # interface do assistente
│  ├─ LocalMachineAssistant.kt  # implementação offline
│  └─ ExternalAiAssistant.kt    # implementação com API externa
├─ viewmodel/
│  ├─ SudokuGameViewModel.kt, CheckersGameViewModel.kt
│  ├─ ProgressViewModel.kt, SettingsViewModel.kt
└─ ui/
   ├─ theme/ (cores e tema claro/escuro)
   ├─ navigation/AppNavigation.kt
   └─ screens/ (Home, Sudoku, Checkers, Progress, Settings)

app/src/test/java/com/joguecomigo/   # testes de Sudoku e Dama
```

---

## 🚀 Como compilar localmente

Pré-requisitos: **JDK 17** e o **Android SDK** (plataforma 34).

```bash
# Clonar o repositório
git clone https://github.com/wvfc/jogos-diversos.git
cd jogos-diversos

# Gerar o APK de debug
./gradlew assembleDebug
```

O APK é gerado em:

```
app/build/outputs/apk/debug/app-debug.apk
```

Para rodar os testes unitários (validação de Sudoku e Dama):

```bash
./gradlew testDebugUnitTest
```

> **Dica:** se estiver fora do Android Studio, crie um arquivo `local.properties`
> na raiz apontando para o seu SDK, por exemplo:
> `sdk.dir=/caminho/para/Android/sdk`

---

## 📦 Gerar o APK automaticamente pelo GitHub Actions

O projeto inclui o workflow `.github/workflows/android-build.yml`, que roda a
cada **push** ou **pull request** na branch `main` (e também pode ser disparado
manualmente em **Actions → Android Build → Run workflow**).

O workflow:
1. Usa `ubuntu-latest`.
2. Configura o **JDK 17**.
3. Dá permissão de execução ao `gradlew`.
4. Executa `./gradlew clean` e `./gradlew assembleDebug`.
5. Publica o APK como artefato chamado **`Jogue-Comigo-APK`**.

### Onde baixar o APK gerado

1. Acesse a aba **Actions** do repositório no GitHub.
2. Clique na execução mais recente do workflow **Android Build**.
3. Na seção **Artifacts** (final da página), baixe **`Jogue-Comigo-APK`**.
4. Descompacte o `.zip` para obter o `app-debug.apk` e instale no aparelho
   (é preciso permitir a instalação de apps de fontes desconhecidas).

---

## 📤 Como enviar para o GitHub

```bash
git add .
git commit -m "Aplicativo Jogue Comigo"
git push -u origin main
```

Ao chegar na branch `main`, o GitHub Actions iniciará o build e disponibilizará
o APK como artefato (ver seção acima).

---

## 🤖 Modo IA (opcional)

Por padrão o app usa a **máquina local** (offline). Para usar a IA externa:

1. Vá em **Configurações → Modo de jogo → Com auxílio de IA**.
2. Em **IA externa**, informe sua **chave de API** (formato compatível com a
   API de *chat completions* da OpenAI) e, se necessário, a **URL** da API.
3. A chave fica salva **apenas no dispositivo** e nunca é embutida no código.

Sem chave ou sem Internet, o app volta automaticamente para o assistente local.

---

## 🎯 Sistema de progressão (resumo)

- Cada partida gera **XP** com base em resultado, dificuldade, tempo, erros e
  dicas usadas. Desempenho fraco gera pouco XP — então o jogador não avança.
- A cada **100 de XP** o jogador sobe de nível. As dificuldades desbloqueiam:
  - Níveis 1–5 → **Fácil**
  - Níveis 6–12 → **Médio**
  - Níveis 13–20 → **Difícil**
  - Nível 21+ → **Especialista**
- O jogador pode escolher manualmente qualquer dificuldade já desbloqueada.
