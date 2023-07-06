module "s3" {
  source  = "github.com/champ-oss/terraform-aws-s3.git?ref=v1.0.38-de0af1f"
  git     = substr(var.git, 0, 25)
  name    = "card-${var.metabase_card_id}"
  protect = var.protect
  tags    = merge(local.tags, var.tags)
}

resource "aws_s3_bucket_notification" "this" {
  bucket = module.s3.bucket

  topic {
    topic_arn = aws_sns_topic.this.arn
    events    = ["s3:ObjectCreated:*"]
  }
}
