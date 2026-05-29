import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class Service {
  String id;
  String name;
  String description;
  double price;
  int durationMinutes;
  bool isBundle;

  Service({
    required this.id,
    required this.name,
    required this.description,
    required this.price,
    required this.durationMinutes,
    required this.isBundle,
  });

  factory Service.fromMap(Map<String, dynamic> data, String docId) {
    return Service(
      id: docId,
      name: data['name'] ?? '',
      description: data['description'] ?? '',
      price: (data['price'] ?? 0.0).toDouble(),
      durationMinutes: data['durationMinutes'] ?? 0,
      isBundle: data['isBundle'] ?? data['bundle'] ?? false,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'price': price,
      'durationMinutes': durationMinutes,
      'isBundle': isBundle,
      'bundle': isBundle,
    };
  }
}

class ManageServicesScreen extends StatefulWidget {
  @override
  _ManageServicesScreenState createState() => _ManageServicesScreenState();
}

class _ManageServicesScreenState extends State<ManageServicesScreen> {
  final _firestore = FirebaseFirestore.instance;
  final _auth = FirebaseAuth.instance;
  bool _isCreatingStarterProfile = false;
  String? _starterProfileError;

  @override
  Widget build(BuildContext context) {
    final uid = _auth.currentUser?.uid;
    if (uid == null)
      return Scaffold(body: Center(child: Text("Please sign in")));

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              "Manage Menu",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 18,
                color: Colors.black,
              ),
            ),
            Text(
              "Set services, add-ons & packages",
              style: TextStyle(fontSize: 12, color: Colors.grey[600]),
            ),
          ],
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: IconThemeData(color: Colors.black),
      ),
      backgroundColor: Colors.white,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showServiceDialog(uid: uid),
        backgroundColor: Theme.of(context).primaryColor,
        icon: Icon(Icons.add),
        label: Text("Add Service/Add-on"),
      ),
      body: StreamBuilder<DocumentSnapshot>(
        stream: _firestore.collection('stylists').doc(uid).snapshots(),
        builder: (context, snapshot) {
          if (!snapshot.hasData)
            return Center(child: CircularProgressIndicator());

          final data = snapshot.data!.data() as Map<String, dynamic>?;
          if (data == null) {
            _ensureStarterStylistProfile(uid);
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (_starterProfileError == null)
                    CircularProgressIndicator()
                  else
                    Icon(Icons.error_outline, color: Colors.red, size: 40),
                  SizedBox(height: 12),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24.0),
                    child: Text(
                      _starterProfileError ?? "Setting up your service menu...",
                      textAlign: TextAlign.center,
                    ),
                  ),
                ],
              ),
            );
          }

          final servicesList = data['services'] as List<dynamic>? ?? [];
          final services = servicesList
              .map((e) => Service.fromMap(e, e['id'] ?? ''))
              .toList();

          final aLaCarte = services.where((s) => !s.isBundle).toList();
          final bundles = services.where((s) => s.isBundle).toList();

          if (services.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.playlist_add, size: 80, color: Colors.grey),
                  SizedBox(height: 16),
                  Text(
                    "Your Menu is Empty",
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                  ),
                  Text(
                    "Add services so clients can book you",
                    style: TextStyle(color: Colors.grey),
                  ),
                ],
              ),
            );
          }

          return ListView(
            padding: EdgeInsets.all(20),
            children: [
              if (bundles.isNotEmpty) ...[
                Text(
                  "Service Bundles & Packages",
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                    color: Theme.of(context).primaryColor,
                  ),
                ),
                SizedBox(height: 12),
                ...bundles.map((b) => _buildServiceTile(b)).toList(),
              ],
              if (aLaCarte.isNotEmpty) ...[
                SizedBox(height: 24),
                Text(
                  "A La Carte Services",
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
                SizedBox(height: 12),
                ...aLaCarte.map((s) => _buildServiceTile(s)).toList(),
              ],
            ],
          );
        },
      ),
    );
  }

  Widget _buildServiceTile(Service service) {
    return Card(
      elevation: 0,
      color: service.isBundle ? Colors.purple[50] : Colors.grey[100],
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      margin: EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: service.isBundle
                  ? Colors.purple[100]
                  : Colors.blue[100],
              child: Icon(
                service.isBundle ? Icons.card_giftcard : Icons.content_cut,
                color: service.isBundle ? Colors.purple : Colors.blue,
              ),
            ),
            SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    service.name,
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                  if (service.isBundle && service.description.isNotEmpty)
                    Text(
                      service.description,
                      style: TextStyle(color: Colors.grey[600], fontSize: 12),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  Text(
                    "${service.durationMinutes} mins • \$${service.price.toStringAsFixed(2)}",
                    style: TextStyle(color: Colors.grey[600], fontSize: 13),
                  ),
                ],
              ),
            ),
            IconButton(
              icon: Icon(Icons.edit, color: Colors.grey),
              onPressed: () => _showServiceDialog(
                uid: _auth.currentUser!.uid,
                existingService: service,
              ),
            ),
            IconButton(
              icon: Icon(Icons.delete_outline, color: Colors.red),
              onPressed: () => _deleteService(service),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _ensureStarterStylistProfile(String uid) async {
    if (_isCreatingStarterProfile) return;
    _isCreatingStarterProfile = true;
    _starterProfileError = null;

    try {
      await _createStarterStylistProfile(uid);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _starterProfileError =
            "We couldn't set up your stylist profile. Please check your connection and reopen this screen.";
      });
    } finally {
      _isCreatingStarterProfile = false;
    }
  }

  Future<void> _createStarterStylistProfile(String uid) async {
    final user = _auth.currentUser;
    final starterName = user?.displayName?.trim().isNotEmpty == true
        ? user!.displayName!.trim()
        : 'Stylist';

    await _firestore.collection('stylists').doc(uid).set({
      'name': starterName,
      'email': user?.email ?? '',
      'role': 'STYLIST',
      'online': false,
      'availableNow': false,
      'available': false,
      'subscriptionActive': false,
      'services': <Map<String, dynamic>>[],
      'categories': ['hair'],
      'servesGender': ['Men', 'Women', 'Non-binary'],
      'serviceLocationType': 'mobile',
      'offersAtHomeService': true,
      'atHomeServiceFee': 20.0,
      'maxTravelRangeKm': 15,
      'createdAt': FieldValue.serverTimestamp(),
      'updatedAt': FieldValue.serverTimestamp(),
    }, SetOptions(merge: true));
  }

  Future<void> _showServiceDialog({
    required String uid,
    Service? existingService,
  }) async {
    final nameController = TextEditingController(
      text: existingService?.name ?? '',
    );
    final descriptionController = TextEditingController(
      text: existingService?.description ?? '',
    );
    final priceController = TextEditingController(
      text: existingService == null
          ? ''
          : existingService.price.toStringAsFixed(2),
    );
    final durationController = TextEditingController(
      text: existingService?.durationMinutes.toString() ?? '60',
    );
    var isBundle = existingService?.isBundle ?? false;

    final saved = await showDialog<Service>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: Text(
                existingService == null ? 'Add Service' : 'Edit Service',
              ),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextField(
                      controller: nameController,
                      decoration: InputDecoration(
                        labelText: 'Service or add-on name',
                      ),
                      textCapitalization: TextCapitalization.words,
                    ),
                    TextField(
                      controller: descriptionController,
                      decoration: InputDecoration(labelText: 'Description'),
                      minLines: 2,
                      maxLines: 3,
                    ),
                    TextField(
                      controller: priceController,
                      decoration: InputDecoration(labelText: 'Price'),
                      keyboardType: TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                    ),
                    TextField(
                      controller: durationController,
                      decoration: InputDecoration(
                        labelText: 'Duration minutes',
                      ),
                      keyboardType: TextInputType.number,
                    ),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('Package or bundle'),
                      value: isBundle,
                      onChanged: (value) =>
                          setDialogState(() => isBundle = value),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () {
                    final name = nameController.text.trim();
                    final price = double.tryParse(priceController.text.trim());
                    final duration =
                        int.tryParse(durationController.text.trim()) ?? 60;
                    if (name.isEmpty || price == null || price <= 0) return;

                    Navigator.pop(
                      context,
                      Service(
                        id:
                            existingService?.id ??
                            DateTime.now().millisecondsSinceEpoch.toString(),
                        name: name,
                        description: descriptionController.text.trim(),
                        price: price,
                        durationMinutes: duration,
                        isBundle: isBundle,
                      ),
                    );
                  },
                  child: Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );

    nameController.dispose();
    descriptionController.dispose();
    priceController.dispose();
    durationController.dispose();

    if (saved == null) return;
    await _upsertService(uid, saved);
  }

  Future<void> _upsertService(String uid, Service service) async {
    final ref = _firestore.collection('stylists').doc(uid);
    await _firestore.runTransaction((transaction) async {
      final doc = await transaction.get(ref);
      final data = doc.data() ?? {};
      final rawServices = List<dynamic>.from(data['services'] as List? ?? []);
      final services = rawServices
          .map((item) => Map<String, dynamic>.from(item as Map))
          .toList();
      final index = services.indexWhere((item) => item['id'] == service.id);
      if (index >= 0) {
        services[index] = service.toMap();
      } else {
        services.add(service.toMap());
      }
      transaction.set(ref, {'services': services}, SetOptions(merge: true));
    });
  }

  Future<void> _deleteService(Service service) async {
    final uid = _auth.currentUser?.uid;
    if (uid == null) return;

    final shouldDelete = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Delete service?'),
        content: Text('Remove ${service.name} from your menu.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text('Delete', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
    if (shouldDelete != true) return;

    final ref = _firestore.collection('stylists').doc(uid);
    await _firestore.runTransaction((transaction) async {
      final doc = await transaction.get(ref);
      final data = doc.data() ?? {};
      final rawServices = List<dynamic>.from(data['services'] as List? ?? []);
      final services = rawServices
          .map((item) => Map<String, dynamic>.from(item as Map))
          .where((item) => item['id'] != service.id)
          .toList();
      transaction.set(ref, {'services': services}, SetOptions(merge: true));
    });
  }
}
