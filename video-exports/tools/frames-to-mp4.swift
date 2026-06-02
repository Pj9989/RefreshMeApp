import AppKit
import AVFoundation
import CoreVideo
import Foundation

let args = CommandLine.arguments
guard args.count >= 6 else {
    fputs("Usage: frames-to-mp4 <framesDir> <output.mp4> <fps> <width> <height>\n", stderr)
    exit(1)
}

let framesDir = URL(fileURLWithPath: args[1])
let outputURL = URL(fileURLWithPath: args[2])
let fps = Int32(args[3]) ?? 30
let width = Int(args[4]) ?? 1080
let height = Int(args[5]) ?? 1920

let fileManager = FileManager.default
try? fileManager.removeItem(at: outputURL)

let frames = try fileManager.contentsOfDirectory(at: framesDir, includingPropertiesForKeys: nil)
    .filter { $0.pathExtension.lowercased() == "jpg" || $0.pathExtension.lowercased() == "png" }
    .sorted { $0.lastPathComponent < $1.lastPathComponent }

guard !frames.isEmpty else {
    fputs("No frames found in \(framesDir.path)\n", stderr)
    exit(1)
}

let writer = try AVAssetWriter(outputURL: outputURL, fileType: .mp4)
let settings: [String: Any] = [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
    AVVideoCompressionPropertiesKey: [
        AVVideoAverageBitRateKey: 12_000_000,
        AVVideoProfileLevelKey: AVVideoProfileLevelH264HighAutoLevel
    ]
]

let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
input.expectsMediaDataInRealTime = false
let adaptor = AVAssetWriterInputPixelBufferAdaptor(
    assetWriterInput: input,
    sourcePixelBufferAttributes: [
        kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
        kCVPixelBufferWidthKey as String: width,
        kCVPixelBufferHeightKey as String: height
    ]
)

guard writer.canAdd(input) else {
    fputs("Cannot add video input\n", stderr)
    exit(1)
}
writer.add(input)

guard writer.startWriting() else {
    fputs("Could not start writer: \(writer.error?.localizedDescription ?? "unknown error")\n", stderr)
    exit(1)
}
writer.startSession(atSourceTime: .zero)

func makePixelBuffer(from image: NSImage) -> CVPixelBuffer? {
    var pixelBuffer: CVPixelBuffer?
    let options: [String: Any] = [
        kCVPixelBufferCGImageCompatibilityKey as String: true,
        kCVPixelBufferCGBitmapContextCompatibilityKey as String: true
    ]
    CVPixelBufferCreate(kCFAllocatorDefault, width, height, kCVPixelFormatType_32ARGB, options as CFDictionary, &pixelBuffer)
    guard let buffer = pixelBuffer else { return nil }

    CVPixelBufferLockBaseAddress(buffer, [])
    defer { CVPixelBufferUnlockBaseAddress(buffer, []) }

    guard let context = CGContext(
        data: CVPixelBufferGetBaseAddress(buffer),
        width: width,
        height: height,
        bitsPerComponent: 8,
        bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
        space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue
    ) else {
        return nil
    }

    context.setFillColor(NSColor.black.cgColor)
    context.fill(CGRect(x: 0, y: 0, width: width, height: height))

    guard let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
        return nil
    }
    context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
    return buffer
}

for (index, frameURL) in frames.enumerated() {
    while !input.isReadyForMoreMediaData {
        Thread.sleep(forTimeInterval: 0.01)
    }

    guard let image = NSImage(contentsOf: frameURL),
          let buffer = makePixelBuffer(from: image) else {
        fputs("Could not read frame \(frameURL.path)\n", stderr)
        exit(1)
    }

    let presentationTime = CMTime(value: CMTimeValue(index), timescale: fps)
    guard adaptor.append(buffer, withPresentationTime: presentationTime) else {
        fputs("Could not append frame \(index): \(writer.error?.localizedDescription ?? "unknown error")\n", stderr)
        exit(1)
    }
}

input.markAsFinished()
let semaphore = DispatchSemaphore(value: 0)
writer.finishWriting {
    semaphore.signal()
}
semaphore.wait()

if writer.status != .completed {
    fputs("Writer failed: \(writer.error?.localizedDescription ?? "unknown error")\n", stderr)
    exit(1)
}

print(outputURL.path)
