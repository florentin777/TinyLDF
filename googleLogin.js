function handleCredentialResponse(response) {
  // Log de la réponse (ID Token brut)
  console.log("ID Token brut : " + response.credential);
  console.log("callback called:" + response.credential);

  let userId = null;

  try {
      // Décoder le token JWT
      const decodedToken = jwt_decode(response.credential);
      console.log("Token JWT décodé :", decodedToken);
      console.log("ID: " + decodedToken.sub);
      console.log('Full Name: ' + decodedToken.name);
      console.log('Given Name: ' + decodedToken.given_name);
      console.log('Family Name: ' + decodedToken.family_name);
      console.log("Image URL: " + decodedToken.picture);
      console.log("Email: " + decodedToken.email);

      // Extraire userId depuis le token décodé
      userId = decodedToken.sub;

  } catch (error) {
      console.error("Erreur lors du décodage du token JWT :", error);
      return; // Arrêter l'exécution en cas d'erreur
  }

  // Envoyer le token et l'userId au serveur
  fetch('/_ah/api/myTinyLDF/v1/addUser', {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify({
          token: response.credential,
          userId: userId
      })
  })
  .then(response => {
      if (!response.ok) {
          throw new Error('Erreur lors de la création de l’utilisateur.');
      }
      return response.json();
  })
  .then(data => {
      alert(`Utilisateur ajouté : ${data.properties.mail}`);
  })
  .catch(error => {
      console.error('Erreur:', error);
  });
}
