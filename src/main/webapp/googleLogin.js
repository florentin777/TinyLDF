function handleCredentialResponse(response) {
    // Log de la réponse (ID Token brut)
    console.log("ID Token brut : " + response.credential);
    console.log("callback called:"+response.credential);
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
    } catch (error) {
      console.error("Erreur lors du décodage du token JWT :", error);
    }
  
    // Vous pouvez envoyer ce token à votre serveur ou effectuer d'autres actions
    // Exemple : envoyer à un serveur pour validation
    // fetch('/validate-token', { method: 'POST', body: response.credential });
  }

  