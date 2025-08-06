<img src="https://img.shields.io/github/v/release/clementfornes13/ExcaliaVoteMod?style=flat-square" alt="Version"/>

<div align="center">
  <img src="https://www.excalia.fr/storage/img/logoexcav3.png" alt="Excalia Logo" width="180"/>

  <h1>Excalia Vote Mod</h1>
    <p><strong>Un mod Minecraft pour afficher vos statistiques de vote mensuelles en jeu sur le serveur Excalia.fr</strong></p>
    <p><strong style="color:red;">
⚠️ Ce mod a été développé de manière indépendante par un joueur de la communauté.  
Je ne fais pas partie de la société Excalia et je ne suis en rien affilié à leur équipe.  
</strong></p>

  
</div>


## Fonctionnalités

- Affiche votre **nombre de votes mensuels**  
- Montre le **temps restant avant le prochain vote disponible**  
- Choisissez parmi **6 styles d'affichage**  
- Interface configurable via raccourcis claviers ou menu dédié

## Installation

1. Installez [**Fabric Loader**](https://fabricmc.net/use/) et [**Fabric API**](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
2. Téléchargez `excaliavotemod-<version>.jar` depuis la [page des releases](https://github.com/clementfornes13/ExcaliaVoteMod/releases)
3. Placez le `.jar` dans le dossier `mods/` de votre installation Minecraft
4. Lancez le jeu avec le profil Fabric


## Raccourcis par défaut

| Touche              | Action                                 |
|---------------------|----------------------------------------|
| `H`                 | Afficher / Masquer le HUD              |
| `+` / `-` (Numpad)  | Agrandir / Réduire l'échelle           |
| `J`                 | Changer de style                       |
| `K`                 | Changer la position                    |
| `O`                 | Ouvrir l'écran de configuration        |
| `R`                 | Réinitialiser la configuration         |


## Configuration

Le fichier est généré automatiquement : `config/excaliavotemod.json`  
Vous pouvez le modifier manuellement ou via l'interface de configuration du mod.

```json
{
  "hudScale": 1.0,
  "styleIndex": 0,
  "hudAnchor": 0
}
```

## Contributions
Vous pouvez contribuer au projet en ouvrant des issues ou en soumettant des pull requests.

  
N'hésitez pas à signaler des bugs ou à proposer des améliorations.