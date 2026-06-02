import 'dart:io';

import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

class ManageBeforeAfterScreen extends StatefulWidget {
  const ManageBeforeAfterScreen({super.key});

  @override
  State<ManageBeforeAfterScreen> createState() => _ManageBeforeAfterScreenState();
}

class _ManageBeforeAfterScreenState extends State<ManageBeforeAfterScreen> {
  final _auth = FirebaseAuth.instance;
  final _firestore = FirebaseFirestore.instance;
  final _storage = FirebaseStorage.instance;

  bool _isLoading = true;
  bool _isUploading = false;
  List<Map<String, dynamic>> _items = const [];

  String get _stylistUid {
    final uid = _auth.currentUser?.uid;
    if (uid == null) throw StateError('Please sign in before uploading.');
    return uid;
  }

  @override
  void initState() {
    super.initState();
    _loadItems();
  }

  Future<void> _loadItems() async {
    setState(() => _isLoading = true);
    try {
      final snapshot =
          await _firestore.collection('stylists').doc(_stylistUid).get();
      final rawItems = snapshot.data()?['beforeAfterImages'] as List<dynamic>?;
      _items = rawItems
              ?.whereType<Map>()
              .map((item) => Map<String, dynamic>.from(item))
              .toList() ??
          [];
    } catch (_) {
      _showMessage('Failed to load before and after photos.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _addTransformation() async {
    final result = await showDialog<_BeforeAfterDraft>(
      context: context,
      barrierDismissible: !_isUploading,
      builder: (_) => const _BeforeAfterDialog(),
    );
    if (result == null) return;

    setState(() => _isUploading = true);
    try {
      final id = DateTime.now().microsecondsSinceEpoch.toString();
      final metadata = SettableMetadata(contentType: 'image/jpeg');
      final beforeRef =
          _storage.ref('before_after/$_stylistUid/${id}_before.jpg');
      final afterRef = _storage.ref('before_after/$_stylistUid/${id}_after.jpg');

      await beforeRef.putFile(File(result.before.path), metadata);
      final beforeUrl = await beforeRef.getDownloadURL();

      await afterRef.putFile(File(result.after.path), metadata);
      final afterUrl = await afterRef.getDownloadURL();

      final item = {
        'id': id,
        'beforeImageUrl': beforeUrl,
        'afterImageUrl': afterUrl,
        'description': result.description,
        'technicalNotes': result.technicalNotes,
        'timestamp': Timestamp.now(),
      };

      await _firestore.collection('stylists').doc(_stylistUid).set({
        'beforeAfterImages': FieldValue.arrayUnion([item]),
      }, SetOptions(merge: true));

      _showMessage('Uploaded successfully!');
      await _loadItems();
    } on FirebaseException catch (error) {
      _showMessage('Upload failed: ${error.message ?? error.code}');
    } catch (error) {
      _showMessage('Upload failed: $error');
    } finally {
      if (mounted) setState(() => _isUploading = false);
    }
  }

  Future<void> _deleteItem(Map<String, dynamic> item) async {
    final id = item['id'] as String?;
    if (id == null) return;

    try {
      final remaining = _items.where((entry) => entry['id'] != id).toList();
      await _firestore.collection('stylists').doc(_stylistUid).set({
        'beforeAfterImages': remaining,
      }, SetOptions(merge: true));

      for (final key in ['beforeImageUrl', 'afterImageUrl']) {
        final url = item[key] as String?;
        if (url == null || url.isEmpty) continue;
        try {
          await _storage.refFromURL(url).delete();
        } catch (_) {
          // File may already be gone; the Firestore record is the source of truth.
        }
      }

      await _loadItems();
      _showMessage('Deleted.');
    } catch (_) {
      _showMessage('Failed to delete.');
    }
  }

  void _showMessage(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Before & After')),
      floatingActionButton: FloatingActionButton(
        onPressed: _isUploading ? null : _addTransformation,
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
          : _items.isEmpty
              ? const Center(
                  child: Text(
                    'No transformations yet.\nTap + to add one.',
                    textAlign: TextAlign.center,
                  ),
                )
              : ListView.separated(
                  padding: const EdgeInsets.all(16),
                  itemCount: _items.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 12),
                  itemBuilder: (context, index) {
                    final item = _items[index];
                    return Card(
                      child: Padding(
                        padding: const EdgeInsets.all(12),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: _LabeledImage(
                                    label: 'Before',
                                    url: item['beforeImageUrl'] as String? ?? '',
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: _LabeledImage(
                                    label: 'After',
                                    url: item['afterImageUrl'] as String? ?? '',
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 10),
                            Text(
                              item['description'] as String? ?? '',
                              style:
                                  const TextStyle(fontWeight: FontWeight.w600),
                            ),
                            if ((item['technicalNotes'] as String? ?? '')
                                .isNotEmpty)
                              Padding(
                                padding: const EdgeInsets.only(top: 4),
                                child: Text(
                                  item['technicalNotes'] as String,
                                  style: Theme.of(context).textTheme.bodySmall,
                                ),
                              ),
                            Align(
                              alignment: Alignment.centerRight,
                              child: TextButton.icon(
                                onPressed: () => _deleteItem(item),
                                icon: const Icon(Icons.delete_outline),
                                label: const Text('Delete'),
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}

class _LabeledImage extends StatelessWidget {
  final String label;
  final String url;

  const _LabeledImage({required this.label, required this.url});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: Theme.of(context).textTheme.labelMedium),
        const SizedBox(height: 4),
        AspectRatio(
          aspectRatio: 1,
          child: ClipRRect(
            borderRadius: BorderRadius.circular(8),
            child: Image.network(url, fit: BoxFit.cover),
          ),
        ),
      ],
    );
  }
}

class _BeforeAfterDialog extends StatefulWidget {
  const _BeforeAfterDialog();

  @override
  State<_BeforeAfterDialog> createState() => _BeforeAfterDialogState();
}

class _BeforeAfterDialogState extends State<_BeforeAfterDialog> {
  final _picker = ImagePicker();
  final _descriptionController = TextEditingController();
  final _notesController = TextEditingController();
  XFile? _before;
  XFile? _after;

  bool get _canUpload =>
      _before != null && _after != null && _descriptionController.text.isNotEmpty;

  @override
  void dispose() {
    _descriptionController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  Future<void> _pickBefore() async {
    final image = await _picker.pickImage(source: ImageSource.gallery);
    if (image != null) setState(() => _before = image);
  }

  Future<void> _pickAfter() async {
    final image = await _picker.pickImage(source: ImageSource.gallery);
    if (image != null) setState(() => _after = image);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Add Transformation'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Row(
              children: [
                Expanded(
                  child: _PhotoPickerTile(
                    label: 'Before',
                    file: _before,
                    onTap: _pickBefore,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _PhotoPickerTile(
                    label: 'After',
                    file: _after,
                    onTap: _pickAfter,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _descriptionController,
              decoration: const InputDecoration(labelText: 'Description'),
              onChanged: (_) => setState(() {}),
            ),
            TextField(
              controller: _notesController,
              decoration:
                  const InputDecoration(labelText: 'Technical notes optional'),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _canUpload
              ? () => Navigator.pop(
                    context,
                    _BeforeAfterDraft(
                      before: _before!,
                      after: _after!,
                      description: _descriptionController.text.trim(),
                      technicalNotes: _notesController.text.trim(),
                    ),
                  )
              : null,
          child: const Text('Upload'),
        ),
      ],
    );
  }
}

class _PhotoPickerTile extends StatelessWidget {
  final String label;
  final XFile? file;
  final VoidCallback onTap;

  const _PhotoPickerTile({
    required this.label,
    required this.file,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: AspectRatio(
        aspectRatio: 1,
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(8),
          ),
          child: file == null
              ? Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.add_a_photo_outlined),
                    const SizedBox(height: 6),
                    Text(label),
                  ],
                )
              : ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.file(File(file!.path), fit: BoxFit.cover),
                ),
        ),
      ),
    );
  }
}

class _BeforeAfterDraft {
  final XFile before;
  final XFile after;
  final String description;
  final String technicalNotes;

  const _BeforeAfterDraft({
    required this.before,
    required this.after,
    required this.description,
    required this.technicalNotes,
  });
}
