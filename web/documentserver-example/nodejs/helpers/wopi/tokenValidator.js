/**
 *
 * (c) Copyright Ascensio System SIA 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

const wopiValidator = require('@mercadoeletronico/wopi-proof-validator');
const DocManager = require('../docManager');
const reqConsts = require('./request');
const utils = require('./utils');

exports.isValidToken = async (req, res, next) => {
  try {
    req.DocManager = new DocManager(req, res);
    const proofKey = await utils.getProofKey(req.DocManager);
    if (!proofKey) {
      next();
      return;
    }

    const timestamp = req.headers[reqConsts.requestHeaders.Timestamp.toLowerCase()];
    const proof = req.headers[reqConsts.requestHeaders.Proof.toLowerCase()];
    const proofold = req.headers[reqConsts.requestHeaders.ProofOld.toLowerCase()];

    let isValid = false;
    if (!timestamp || !proof || !proofold) {
      console.warn(`Proof keys: timestamp ${timestamp} , proof ${proof} , proofold ${proofold}`);
    } else {
      isValid = wopiValidator.check(
        {
          url: `${req.DocManager.getServerPath()}${req.originalUrl || req.url}`,
          accessToken: req.query.access_token,
          timestamp,
        },
        {
          proof,
          proofold,
        },
        {
          modulus: proofKey.modulus,
          exponent: proofKey.exponent,
          oldmodulus: proofKey.oldmodulus,
          oldexponent: proofKey.oldexponent,
        },
      );
    }
    if (isValid) {
      next();
    } else {
      console.warn('Proof key verification failed');
      res.status(403).send('Not verified');
    }
  } catch (error) {
    console.error(error);
    res.status(500).send(`Verification error: ${error.message}`);
  }
};
