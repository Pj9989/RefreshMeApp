import 'dart:io';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import 'manage_before_after_screen.dart';

class ManagePortfolioScreen extends StatefulWidget {
  const ManagePortfolioScreen({super.key});

  @override
  State<ManagePortfolioScreen> createState() => _ManagePortfolioScreenState();
}

class _ManagePortfolioScreenState extends State<ManagePortfolioScreen> {
  final _auth = FirebaseAuth.instance;
  final _firestore = FirebaseFirestore.instance;
  final _picker = ImagePicker();
  final _storage = FirebaseStorage.instance;

  bool _isLoading = true;
  bool _isUploading = false;
  List<String> _portfolioImages = const [];
  List<String> _portfolioVideos = const [];

  String get _stylistUid {
    final uid = _auth.currentUser?.uid;
    if (uid == null) throw StateError('Please sign in before uploading.');
    return uid;
  }

  @override
  void initState() {
    super.initState();
    _loadPortfolio();
  }

  Future<void> _loadPortfolio() async {
    setState(() => _isLoading = true);
    try {
      final snapshot =
          await _firestore.collection('stylists').doc(_stylistUid).get();
      final data = snapshot.data() ?? {};
      _portfolioImages = _stringList(data['portfolioImages']);
      _portfolioVideos = _stringList(data['portfolioVideos']);
    } catch (_) {
      _showMessage('Failed to load portfolio.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  List<String> _stringList(Object? value) {
    if (value is List) return value.whereType<String>().toList();
    return const [];
  }

  Future<void> _pickAndUpload() async {
    final media = await _picker.pickMedia();
    if (media == null) return;

    setState(() => _isUploading = true);
    try {
      final contentType = _contentTypeFor(media);
      final isVideo = contentType.startsWith('video/');
      final folder = isVideo ? 'portfolio_videos' : 'portfolio_images';
      final field = isVideo ? 'portfolioVideos' : 'portfolioImages';
      final extension = _extensionFor(contentType, media.path);
      final filename = '${DateTime.now().millisecondsSinceEpoch}.$extension';
      final ref = _storage.ref('$folder/$_stylistUid/$filename');

      await ref.putFile(
        File(media.path),
        SettableMetadata(contentType: contentType),
      );
      final downloadUrl = await ref.getDownloadURL();

      await _firestore.collection('stylists').doc(_stylistUid).set({
        field: FieldValue.arrayUnion([downloadUrl]),
      }, SetOptions(merge: true));

      _showMessage('Media uploaded successfully!');
      await _loadPortfolio();
    } on FirebaseException catch (error) {
      _showMessage('Upload failed: ${error.message ?? error.code}');
    } catch (error) {
      _showMessage('Upload failed: $error');
    } finally {
      if (mounted) setState(() => _isUploading = false);
    }
  }

  String _contentTypeFor(XFile file) {
    final mimeType = file.mimeType;
    if (mimeType != null && mimeType.isNotEmpty) return mimeType;

    final path = file.path.toLowerCase();
    if (path.endsWith('.png')) return 'image/png';
    if (path.endsWith('.webp')) return 'image/webp';
    if (path.endsWith('.heic') || path.endsWith('.heif')) return 'image/heic';
    if (path.endsWith('.mov')) return 'video/quicktime';
    if (path.endsWith('.mp4')) return 'video/mp4';
    return 'image/jpeg';
  }

  String _extensionFor(String contentType, String path) {
    switch (contentType) {
      case 'image/png':
        return 'png';
      case 'image/webp':
        return 'webp';
      case 'image/heic':
      case 'image/heif':
        return 'heic';
      case 'video/quicktime':
        return 'mov';
      case 'video/mp4':
        return 'mp4';
    }

    final dot = path.lastIndexOf('.');
    if (dot >= 0 && dot < path.length - 1) {
      return path.substring(dot + 1).toLowerCase();
    }
    return contentType.startsWith('video/') ? 'mp4' : 'jpg';
  }

  Future<void> _deleteMedia(String url, bool isVideo) async {
    final field = isVideo ? 'portfolioVideos' : 'portfolioImages';
    try {
      await _firestore.collection('stylists').doc(_stylistUid).set({
        field: FieldValue.arrayRemove([url]),
      }, SetOptions(merge: true));

      try {
        await _storage.refFromURL(url).delete();
      } catch (_) {
        // File may already be gone; the Firestore list is what users see.
      }

      _showMessage('Media deleted.');
      await _loadPortfolio();
    } catch (_) {
      _showMessage('Failed to delete media.');
    }
  }

  void _openBeforeAfter() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const ManageBeforeAfterScreen()),
    );
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    final mediaCount = _portfolioImages.length + _portfolioVideos.length;

    return Scaffold(
      appBar: AppBar(
        title: const Text('My Portfolio'),
        actions: [
          IconButton(
            onPressed: _openBeforeAfter,
            icon: const Icon(Icons.compare_rounded),
            tooltip: 'Before & After',
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _isUploading ? null : _pickAndUpload,
        child: _isUploading
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : const Icon(Icons.add_photo_alternate_rounded),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : mediaCount == 0
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text(
                        'No portfolio media yet.\nTap + to add photos and videos.',
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 16),
                      OutlinedButton.icon(
                        onPressed: _openBeforeAfter,
                        icon: const Icon(Icons.compare_rounded),
                        label: const Text('Before & After'),
                      ),
                    ],
                  ),
                )
              : Column(
                  children: [
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton.icon(
                        onPressed: _openBeforeAfter,
                        icon: const Icon(Icons.compare_rounded),
                        label: const Text('Before & After'),
                      ),
                    ),
                    Expanded(
                      child: GridView.builder(
                        padding: const EdgeInsets.all(8),
                        gridDelegate:
                            const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3,
                          crossAxisSpacing: 8,
                          mainAxisSpacing: 8,
                        ),
                        itemCount: mediaCount,
                        itemBuilder: (context, index) {
                          final isVideo = index < _portfolioVideos.length;
                          final url = isVideo
                              ? _portfolioVideos[index]
                              : _portfolioImages[index - _portfolioVideos.length];
                          return _PortfolioTile(
                            url: url,
                            isVideo: isVideo,
                            onDelete: () => _deleteMedia(url, isVideo),
                          );
                        },
                      ),
                    ),
                  ],
                ),
    );
  }
}

class _PortfolioTile extends StatelessWidget {
  final String url;
  final bool isVideo;
  final VoidCallback onDelete;

  const _PortfolioTile({
    required this.url,
    required this.isVideo,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Stack(
        fit: StackFit.expand,
        children: [
          if (isVideo)
            ColoredBox(
              color: Colors.black87,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: const [
                  Icon(Icons.play_circle_fill_rounded,
                      color: Colors.white, size: 40),
                  SizedBox(height: 4),
                  Text('REEL', style: TextStyle(color: Colors.white)),
                ],
              ),
            )
          else
            Image.network(url, fit: BoxFit.cover),
          Positioned(
            top: 4,
            right: 4,
            child: InkWell(
              onTap: onDelete,
              borderRadius: BorderRadius.circular(20),
              child: Container(
                width: 28,
                height: 28,
                decoration: const BoxDecoration(
                  color: Colors.black54,
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.delete_outline,
                  color: Colors.white,
                  size: 16,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
