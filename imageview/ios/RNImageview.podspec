
Pod::Spec.new do |s|
  s.name         = "RNImageView"
  s.version      = "1.0.0"
  s.summary      = "RNImageView"
  s.description  = <<-DESC
                  RNImageview
                   DESC
  s.homepage     = "https://github.com/zhuangch/rn_imageview/tree/master/imageview"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/zhuangch/rn_imageview.git", :tag => "main" }
  s.source_files  = "*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  s.dependency "SDWebImage"

end

  