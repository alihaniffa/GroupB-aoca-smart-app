Pod::Spec.new do |spec|
  spec.name         = 'FirebaseFirestoreBridge'
  spec.version      = '1.0.0'
  spec.summary      = 'Objective-C bridge for Firebase Firestore'
  spec.homepage     = 'https://example.com'
  spec.license      = { :type => 'MIT' }
  spec.author       = { 'Group B' => 'groupb@example.com' }

  spec.platform     = :ios, '18.0'

  spec.source       = {
    :path => '.'
  }

  spec.source_files = 'FirebaseFirestoreBridge.{h,m}'
  spec.public_header_files = 'FirebaseFirestoreBridge.h'

  spec.dependency 'FirebaseFirestore'
end