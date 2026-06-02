/// RefreshMe — Virtual Try-On Service
/// Place this file at: lib/services/virtual_try_on_service.dart

import 'dart:convert';
import 'dart:io';

import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';

class VirtualTryOnService {
  static const String _defaultModelVersion =
      '39ed52f2a78e934b3ba6e2a89f5b1c712de7dfea535525255b1aa35c5565e08b';

  VirtualTryOnService({FirebaseFunctions? functions, FirebaseAuth? auth})
      : _functions = functions ?? FirebaseFunctions.instance,
        _auth = auth ?? FirebaseAuth.instance;

  final FirebaseFunctions _functions;
  final FirebaseAuth _auth;

  Future<String> generateHairstyle({
    required File imageFile,
    required String hairstyle,
    required String gender,
    required String ethnicity,
  }) async {
    final user = _auth.currentUser;
    if (user == null) {
      throw Exception('Please sign in before using virtual try-on.');
    }

    final bytes = await imageFile.readAsBytes();
    final base64Image = base64Encode(bytes);
    final prompt =
        'Apply a realistic $hairstyle hairstyle to this $ethnicity $gender. '
        'Keep the exact same person, face, expression, pose, lighting, '
        'background, and clothing.';

    final callable = _functions.httpsCallable(
      'runVirtualTryOn',
      options: HttpsCallableOptions(timeout: const Duration(seconds: 180)),
    );
    final result = await callable.call(<String, dynamic>{
      'base64Image': base64Image,
      'prompt': prompt,
      'modelVersion': _defaultModelVersion,
      'negativePrompt':
          'different face, altered facial features, face swap, changed identity, '
          'changed expression, professional studio portrait, painting, illustration, '
          'fake, 3d render, morphed, changed background, changed clothing, blur, '
          'oversaturated, beauty filter, plastic skin, watermark, text',
    });

    final data = Map<String, dynamic>.from(result.data as Map);
    final outputUrl = data['outputUrl']?.toString();
    if (outputUrl == null || outputUrl.isEmpty) {
      throw Exception('Virtual try-on did not return an image.');
    }
    return outputUrl;
  }
}
