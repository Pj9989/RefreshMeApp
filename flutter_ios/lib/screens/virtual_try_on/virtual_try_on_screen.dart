/// RefreshMe — Virtual Try-On Screen
/// Place this file at: lib/screens/virtual_try_on/virtual_try_on_screen.dart
///
/// Requires image_picker in pubspec.yaml:
///   image_picker: ^1.1.2

import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../services/virtual_try_on_service.dart';

class VirtualTryOnScreen extends StatefulWidget {
  const VirtualTryOnScreen({super.key});

  @override
  State<VirtualTryOnScreen> createState() => _VirtualTryOnScreenState();
}

class _VirtualTryOnScreenState extends State<VirtualTryOnScreen> {
  final _picker = ImagePicker();
  final _service = VirtualTryOnService();

  File? _imageFile;
  String? _outputUrl;
  String? _error;
  bool _isGenerating = false;

  String _hairstyle = 'Balayage Waves';
  String _gender = 'Female';
  String _ethnicity = 'Caucasian';

  static const _hairstyles = <String>[
    'Balayage Waves',
    'Pixie Cut',
    'Bob Cut',
    'Curtain Bangs',
    'Buzz Cut',
    'Fade',
    'Locs',
    'Braids',
    'Layered Shag',
  ];
  static const _genders = <String>['Female', 'Male', 'Non-Binary'];
  static const _ethnicities = <String>[
    'Caucasian',
    'African American',
    'Asian',
    'Hispanic',
    'Middle Eastern',
    'Mixed',
  ];

  Future<void> _pickImage() async {
    final picked = await _picker.pickImage(
      source: ImageSource.photoLibrary,
      maxWidth: 1536,
      maxHeight: 1536,
      imageQuality: 92,
    );
    if (picked == null) return;
    setState(() {
      _imageFile = File(picked.path);
      _outputUrl = null;
      _error = null;
    });
  }

  Future<void> _generate() async {
    final imageFile = _imageFile;
    if (imageFile == null || _isGenerating) return;

    setState(() {
      _isGenerating = true;
      _error = null;
    });

    try {
      final outputUrl = await _service.generateHairstyle(
        imageFile: imageFile,
        hairstyle: _hairstyle,
        gender: _gender,
        ethnicity: _ethnicity,
      );
      if (!mounted) return;
      setState(() {
        _outputUrl = outputUrl;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.toString().replaceFirst('Exception: ', '');
      });
    } finally {
      if (mounted) {
        setState(() {
          _isGenerating = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('AI Virtual Try-On')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AspectRatio(
            aspectRatio: 1,
            child: InkWell(
              borderRadius: BorderRadius.circular(24),
              onTap: _isGenerating ? null : _pickImage,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(24),
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerHighest,
                  ),
                  child: _buildPreview(),
                ),
              ),
            ),
          ),
          const SizedBox(height: 20),
          _PickerRow(
            label: 'Style',
            value: _hairstyle,
            values: _hairstyles,
            onChanged: (value) => setState(() => _hairstyle = value),
          ),
          _PickerRow(
            label: 'Gender',
            value: _gender,
            values: _genders,
            onChanged: (value) => setState(() => _gender = value),
          ),
          _PickerRow(
            label: 'Ethnicity',
            value: _ethnicity,
            values: _ethnicities,
            onChanged: (value) => setState(() => _ethnicity = value),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: _imageFile == null || _isGenerating ? null : _generate,
            icon: _isGenerating
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.auto_awesome),
            label: Text(_isGenerating ? 'Generating...' : 'Generate My Look'),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(
              _error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildPreview() {
    if (_isGenerating) {
      return Stack(
        fit: StackFit.expand,
        children: [
          if (_imageFile != null) Image.file(_imageFile!, fit: BoxFit.cover),
          ColoredBox(color: Colors.black.withOpacity(0.45)),
          const Center(child: CircularProgressIndicator()),
        ],
      );
    }
    if (_outputUrl != null) {
      return Image.network(_outputUrl!, fit: BoxFit.cover);
    }
    if (_imageFile != null) {
      return Image.file(_imageFile!, fit: BoxFit.cover);
    }
    return const Center(child: Icon(Icons.add_a_photo, size: 48));
  }
}

class _PickerRow extends StatelessWidget {
  const _PickerRow({
    required this.label,
    required this.value,
    required this.values,
    required this.onChanged,
  });

  final String label;
  final String value;
  final List<String> values;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: DropdownButtonFormField<String>(
        value: value,
        decoration: InputDecoration(labelText: label),
        items: values
            .map((item) => DropdownMenuItem(value: item, child: Text(item)))
            .toList(),
        onChanged: (item) {
          if (item != null) onChanged(item);
        },
      ),
    );
  }
}
