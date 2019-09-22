const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();

exports.modifyPlayer = functions.firestore
    .document('places/{place}/playing/{playerID}')
    .onWrite((change, context) => {
      const place = context.params.place;
      const player = change.after.exists ? change.after.data() : null;
      const oldDoc = change.before.data();

      console.log('players changed');

      let deleted = player === null;
      let playerName = deleted ? oldDoc.name : player.name;
      let playerPhotoUrl = deleted ? oldDoc.photoUrl : player.photoUrl;

      const payload = {
        notification: {
          title: !deleted ? `New ${place} player!` : `${place} player leaves...`,
          body: `${playerName} checked ` + (!deleted ? 'in!' : 'out.'),
        }
      };

      return admin.messaging().sendToTopic(place, payload);
    });

exports.addMedia = functions.firestore
    .document('places/{place}/media/{mediaID}')
    .onCreate((snap, context) => {
      const place = context.params.place;
      const media = snap.data();

      console.log('media added');

      const pic = media.owner;

      const payload = {
        notification: {
          title: `New ${place} media added!`,
          body: `${media.owner} added a ` + (pic ? 'picture!' : 'video!'),
        }
      };

      return admin.messaging().sendToTopic(place, payload);
    });

exports.addGame = functions.firestore
    .document('places/{place}/games/{gameID}')
    .onCreate((snap, context) => {
      const place = context.params.place;
      const game = snap.data();

      console.log('game added');

      // const first = db.doc('places/kx/players/' + game.firstUid).get();
      // const second = db.doc('places/kx/players/' + game.secondUid).get();

      const payload = {
        notification: {
          title: `New ${place} game result added!`,
          body: `${game.firstName} vs ${game.secondName}!`,
        }
      };

      return admin.messaging().sendToTopic(place, payload);
    });

exports.addGameResult = functions.firestore
    .document('places/{place}/pendingresults/{resultID}')
    .onCreate((snap, context) => {
      const place = context.params.place;
      const game = snap.data();

      console.log('pending game result added');

      const payload = {
        notification: {
          title: 'You have new game result to approve!',
          body: `${game.firstName} vs ${game.secondName}!`,
        }
      };

      return admin.messaging().sendToDevice(game.secondToken, payload);
    });