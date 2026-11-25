# Lewd Crossing

Mod Minecraft pour Fabric, en deux versions : **1.21.1** et **26.3**. Contenu
réservé aux adultes : au premier lancement, le jeu demande si le joueur a 18 ans
ou plus.

Des compagnes à rencontrer et à apprivoiser, une collection d'objets et de
machines qui détournent les mécaniques du jeu, et une dimension étrange, le
vide, sous les laboratoires des sites de confinement.

## Contenu

- **Trois compagnes** : Jenny, Ellie et FapGPT (l'IA holographique du
  laboratoire). Chacune a sa personnalité, ses répliques, son humeur
  et une jauge d'affection qui ouvre peu à peu de nouvelles interactions
  (discussion, câlin, tenues, demandes).
- **Scènes animées**, avec ou sans participation du joueur, et une barre
  d'excitation.
- **Objets et jouets** : menottes, fouet, barre de pole dance, cloison à trou,
  dildo pogo, Cummy piston, potions de forme et d'inversion, capsule de
  capture, appareil photo et OnlyFaps.
- **Le cum** : fioles, traces au sol et sur les filles, chaudron, fromage,
  plats « au cum », flèches et fioles à jeter, bocal, compresseur et Cum
  Crystal.
- **Le laboratoire et le vide** : terminal (boutique, radio, OnlyFaps),
  distributeur, ascenseur, écrans muraux, combinaison et nage dans le vide,
  lidar, failles, et un marchand inquiétant derrière un arbre.
- **Structures** : maisons dans cinq biomes, sites de confinement.

Les textes du jeu sont en anglais.

## Installation

Télécharger le jar de sa version de Minecraft depuis la page **Releases** du
dépôt et le déposer dans le dossier `mods/` de l'instance.

| Dépendance | Minecraft 1.21.1 | Minecraft 26.3 |
|---|---|---|
| Fabric Loader | 0.17 ou plus | 0.19 ou plus |
| Fabric API | pour 1.21.1 | pour 26.3 |
| GeckoLib | 4.9 ou plus | 5.5 ou plus |
| Java | 21 | 25 |

JEI est pris en charge s'il est installé (recettes d'alambic du mod), sans
être nécessaire. Le mod doit être présent sur le client et sur le serveur.

## Fichiers non publiés

Certains fichiers ne sont **pas publiés** dans ce dépôt : je ne suis pas
certain d'avoir le droit de les redistribuer. Sont absents :

- les modèles 3D, animations et planches de Jenny et d'Ellie, ainsi que ceux
  qui en dérivent (FapGPT, les mains à la première personne) ;
- le modèle de Gaster ;
- les éléments tirés de Deltarune dans la boutique de Gaster (fond, cœur,
  police, bruit d'achat).

Le code et le reste des ressources sont complets, mais un jar compilé depuis
ce dépôt n'affichera pas ces personnages ni la boutique telle qu'en jeu. Pour
jouer, utiliser le jar des Releases.

## Compiler

Version 1.21.1, à la racine :

```bash
./gradlew build
```

Version 26.3, dans `versions/26.3/` (elle reprend les ressources de la racine
et n'y ajoute que ce que le format de 26.3 change) :

```bash
cd versions/26.3
./gradlew build
```

Le jar est écrit dans `build/libs/` de chaque version. Gradle télécharge
lui-même le JDK qu'il faut (21 ou 25) si la machine n'en a pas.

## Crédits

Voir `CREDITS.txt`, également inclus dans le jar : le Jenny Mod de
SchnurriTV, le modèle « Stylized Gaster » de Skyguy555, des éléments de
Deltarune (Toby Fox) dans la boutique de Gaster, et des textures du jeu
retintées (Mojang).

## Licence

Le code est sous licence MIT (`LICENSE`). Les ressources d'autres auteurs
citées dans `CREDITS.txt` restent la propriété de leurs auteurs.
