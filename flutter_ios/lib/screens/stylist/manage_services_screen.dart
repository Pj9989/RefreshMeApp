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
}

class ManageServicesScreen extends StatefulWidget {
  @override
  _ManageServicesScreenState createState() => _ManageServicesScreenState();
}

class _ManageServicesScreenState extends State<ManageServicesScreen> {
  final _firestore = FirebaseFirestore.instance;
  final _auth = FirebaseAuth.instance;

  @override
  Widget build(BuildContext context) {
    final uid = _auth.currentUser?.uid;
    if (uid == null) return Scaffold(body: Center(child: Text("Please sign in")));

    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("Manage Menu", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Colors.black)),
            Text("Set your services & packages", style: TextStyle(fontSize: 12, color: Colors.grey[600])),
          ],
        ),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: IconThemeData(color: Colors.black),
      ),
      backgroundColor: Colors.white,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {
          // Open edit dialog
        },
        backgroundColor: Theme.of(context).primaryColor,
        icon: Icon(Icons.add),
        label: Text("Add Service"),
      ),
      body: StreamBuilder<DocumentSnapshot>(
        stream: _firestore.collection('stylists').doc(uid).snapshots(),
        builder: (context, snapshot) {
          if (!snapshot.hasData) return Center(child: CircularProgressIndicator());

          final data = snapshot.data!.data() as Map<String, dynamic>?;
          if (data == null) return Center(child: Text("No data found"));

          final servicesList = data['services'] as List<dynamic>? ?? [];
          final services = servicesList.map((e) => Service.fromMap(e, e['id'] ?? '')).toList();

          final aLaCarte = services.where((s) => !s.isBundle).toList();
          final bundles = services.where((s) => s.isBundle).toList();

          if (services.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.playlist_add, size: 80, color: Colors.grey),
                  SizedBox(height: 16),
                  Text("Your Menu is Empty", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                  Text("Add services so clients can book you", style: TextStyle(color: Colors.grey)),
                ],
              ),
            );
          }

          return ListView(
            padding: EdgeInsets.all(20),
            children: [
              if (bundles.isNotEmpty) ...[
                Text("Service Bundles & Packages", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Theme.of(context).primaryColor)),
                SizedBox(height: 12),
                ...bundles.map((b) => _buildServiceTile(b)).toList(),
              ],
              if (aLaCarte.isNotEmpty) ...[
                SizedBox(height: 24),
                Text("A La Carte Services", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                SizedBox(height: 12),
                ...aLaCarte.map((s) => _buildServiceTile(s)).toList(),
              ]
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
              backgroundColor: service.isBundle ? Colors.purple[100] : Colors.blue[100],
              child: Icon(service.isBundle ? Icons.card_giftcard : Icons.content_cut, color: service.isBundle ? Colors.purple : Colors.blue),
            ),
            SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(service.name, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  if (service.isBundle && service.description.isNotEmpty)
                    Text(service.description, style: TextStyle(color: Colors.grey[600], fontSize: 12), maxLines: 2, overflow: TextOverflow.ellipsis),
                  Text("${service.durationMinutes} mins • \$${service.price.toStringAsFixed(2)}", style: TextStyle(color: Colors.grey[600], fontSize: 13)),
                ],
              ),
            ),
            IconButton(icon: Icon(Icons.edit, color: Colors.grey), onPressed: () {}),
            IconButton(icon: Icon(Icons.delete_outline, color: Colors.red), onPressed: () {}),
          ],
        ),
      ),
    );
  }
}
